package tv.shout.snowyowl.engine;

import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

import org.apache.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;

import com.meinc.facebook.exception.FacebookAuthenticationNeededException;
import com.meinc.facebook.exception.FacebookGeneralException;
import com.meinc.facebook.exception.FacebookUserExistsException;
import com.meinc.facebook.exception.InvalidAccessTokenException;
import com.meinc.identity.domain.SignupData;
import com.meinc.identity.domain.Subscriber;
import com.meinc.identity.domain.SubscriberSession;
import com.meinc.identity.exception.EmailAlreadyUsedException;
import com.meinc.identity.exception.InvalidEmailException;
import com.meinc.identity.exception.InvalidSessionException;
import com.meinc.identity.exception.NicknameInvalidException;
import com.meinc.identity.service.IIdentityService;

import tv.shout.sc.domain.Game;
import tv.shout.sc.domain.Round;
import tv.shout.sc.service.IShoutContestService;
import tv.shout.snowyowl.dao.IXmlDaoMapper;
import tv.shout.snowyowl.service.CurrentRankCalculator;
import tv.shout.snowyowl.service.ISnowyowlService;
import tv.shout.util.FastMap;

public class BotEngine
extends BaseAiEngine
{
    private static final String BOT_EMAIL_TEMPLATE = "shoutbot_{0,number,#}@shout.tv";
    private static final String BOT_USERNAME_TEMPLATE = "playerbot {0,number,#}";

    private static Logger _logger = Logger.getLogger(BotEngine.class);

    @Autowired
    private IXmlDaoMapper _xmlDao;

    @Autowired
    private IShoutContestService _shoutContestService;

    @Autowired
    private IIdentityService _identityService;

    @Autowired
    private CurrentRankCalculator _currentRankCalculator;

    private Lock _botBusyLock = new ReentrantLock();

    //assumes caller has created a db transaction
    /**
     * @param gameId add bot players to this game
     * @param roundId start them in this round
     * @return the list of bots (subscriberIds) that were added
     */
    public List<Long> addBotPlayers(String gameId, String roundId, int numBotsNeeded)
    {
        //get a list of all available bot players
        List<Long> availableBotIds;
        _botBusyLock.lock();
        try {
            availableBotIds = _dao.getIdleBotIds();
        } finally {
            _botBusyLock.unlock();
        }

        //if there aren't enough bot players, create some
        if (availableBotIds.size() < numBotsNeeded) {
            List<Long> newlyAddedBotIds = createBotPlayers(numBotsNeeded - availableBotIds.size());
            _xmlDao.addBotsToSystem(newlyAddedBotIds);
            availableBotIds.addAll(newlyAddedBotIds);
        }

        //mark the bots as being in the game in the db
        //not using list.sublist on purpose. might cause weirdness if the list is changed later by others
        List<Long> botsNeeded = new ArrayList<>(numBotsNeeded);
        for (int i=0; i<numBotsNeeded; i++) {
            botsNeeded.add(availableBotIds.get(i));
        }

        _xmlDao.addBotsToGame(gameId, botsNeeded);

        //add to the game and make available for pairing
        Game game = _shoutContestService.getGame(gameId);
        Round round = null;
        for (int i=0; i<botsNeeded.size(); i++) {
            long botSubscriberId = botsNeeded.get(i);
            boolean lastCall = i == botsNeeded.size()-1;
            _commonBusinessLogic.joinGameAsBot(game, botSubscriberId, roundId);
            round = _commonBusinessLogic.setPlayerAvailabilityAsBot(game, round, botSubscriberId, lastCall);
        }

        _currentRankCalculator.clear(game.getId());

        return botsNeeded;
    }

    //assumes caller has created a db transaction
    /**
     * @param gameId the game to check
     * @return a list of all the subscriberIds for the given game that are actually bot players
     */
    public List<Long> getBotsForGame(String gameId)
    {
        return _dao.getBotsForGame(gameId);
    }

    private List<Long> createBotPlayers(int numBotPlayers)
    {
        List<Long> botSubscriberIds = new ArrayList<>();

        int contextId = _shoutContestService.getContextId(new FastMap<>("appId", ISnowyowlService.APP_ID));
        int botCount = _dao.getBotCount();

        SignupData signupData = new SignupData();
        signupData.setAppName(ISnowyowlService.APP_ID);
        signupData.setPasswordSet(true);
        signupData.setLanguageCode("en");
        signupData.setFromCountryCode("US");
        signupData.setFirstName("ShoutBot");
        signupData.setDateOfBirth(new Date());
        signupData.setPhotoUrl("https://upload.wikimedia.org/wikipedia/commons/thumb/0/05/Robot_icon.svg/600px-Robot_icon.svg.png");

        SubscriberSession subscriberSession = new SubscriberSession();
        subscriberSession.setDeviceModel("shoutbot");
        subscriberSession.setDeviceName("Shout Bot");
        subscriberSession.setDeviceVersion("1.0");
        subscriberSession.setOsName("ShoutBot");
        subscriberSession.setOsType("dead simple");
        subscriberSession.setAppId(ISnowyowlService.APP_ID);
        subscriberSession.setAppVersion("1.0");

        for (int i=0; i<numBotPlayers; i++) {
            botCount++;

            signupData.setDeviceToken(UUID.randomUUID().toString());
            signupData.setPassword(UUID.randomUUID().toString());
            signupData.setEmail(MessageFormat.format(BOT_EMAIL_TEMPLATE, botCount));
            signupData.setUsername(MessageFormat.format(BOT_USERNAME_TEMPLATE, botCount));
            signupData.setLastName((botCount) + "");

            subscriberSession.setDeviceId(UUID.randomUUID().toString());

            Subscriber bot;
            try {
                bot = _identityService.signup(contextId, signupData, subscriberSession);
                botSubscriberIds.add(bot.getSubscriberId());

            } catch (FacebookGeneralException | FacebookUserExistsException | FacebookAuthenticationNeededException | InvalidAccessTokenException |
                    InvalidSessionException | InvalidEmailException | EmailAlreadyUsedException | NicknameInvalidException e) {
                _logger.error("unexpected error while creating bot", e);
                throw new IllegalStateException("unable to create bot", e);
            }
        }

        return botSubscriberIds;
    }
}
