package hidden;

import java.io.Serializable;

public class Hidden implements Serializable {
  private String message;

  public String getMessage() {
    return message;
  }

  public void setMessage(String message) {
    this.message = message;
  }
}
