/**************************************************************************
 * copyright file="AttachmentCollection.java" company="Microsoft"
 *     Copyright (c) Microsoft Corporation.  All rights reserved.
 * 
 * Defines the AttachmentCollection.java.
 **************************************************************************/
package microsoft.exchange.webservices.data;

import java.io.File;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Enumeration;

/***
 * 
 * Represents an item's attachment collection.
 */
@EditorBrowsable(state = EditorBrowsableState.Never)
public final class AttachmentCollection extends
		ComplexPropertyCollection<Attachment> implements IOwnedProperty {

	// The item owner that owns this attachment collection
	/** The owner. */
	private Item owner;

	/***
	 * Initializes a new instance of AttachmentCollection.
	 */
	protected AttachmentCollection() {
		super();
	}

	/**
	 * The owner of this attachment collection.
	 * 
	 * @return the owner
	 */
	public ServiceObject getOwner() {
		return this.owner;
	}

	/**
	 * The owner of this attachment collection.
	 * 
	 * @param value
	 *            accepts ServiceObject
	 */
	public void setOwner(ServiceObject value) {
		Item item = (Item)value;
		EwsUtilities.EwsAssert(item != null,
				"AttachmentCollection.IOwnedProperty.set_Owner",
				"value is not a descendant of ItemBase");
		
		this.owner = item;
	}

	/**
	 * * Adds a file attachment to the collection.
	 * 
	 * @param fileName
	 *            the file name
	 * @return A FileAttachment instance.
	 */
	public FileAttachment addFileAttachment(String fileName) {
		return this.addFileAttachment(new File(fileName).getName(), fileName);
	}

	/**
	 * * Adds a file attachment to the collection.
	 * 
	 * @param name
	 *            accepts String display name of the new attachment.
	 * @param fileName
	 *            accepts String name of the file representing the content of
	 *            the attachment.
	 * @return A FileAttachment instance.
	 */
	public FileAttachment addFileAttachment(String name, String fileName) {
		FileAttachment fileAttachment = new FileAttachment(this.owner);
		fileAttachment.setName(name);
		fileAttachment.setFileName(fileName);

		this.internalAdd(fileAttachment);

		return fileAttachment;
	}

	/**
	 * * Adds a file attachment to the collection.
	 * 
	 * @param name
	 *            accepts String display name of the new attachment.
	 * @param contentStream
	 *            accepts InputStream stream from which to read the content of
	 *            the attachment.
	 * @return A FileAttachment instance.
	 */
	public FileAttachment addFileAttachment(String name,
			InputStream contentStream) {
		FileAttachment fileAttachment = new FileAttachment(this.owner);
		fileAttachment.setName(name);
		fileAttachment.setContentStream(contentStream);

		this.internalAdd(fileAttachment);

		return fileAttachment;
	}

	/**
	 * * Adds a file attachment to the collection.
	 * 
	 * @param name
	 *            the name
	 * @param content
	 *            accepts byte byte arrays representing the content of the
	 *            attachment.
	 * @return FileAttachment
	 */
	public FileAttachment addFileAttachment(String name, byte[] content) {
		FileAttachment fileAttachment = new FileAttachment(this.owner);
		fileAttachment.setName(name);
		fileAttachment.setContent(content);

		this.internalAdd(fileAttachment);

		return fileAttachment;
	}

	/**
	 * * Adds an item attachment to the collection.
	 * 
	 * @param <TItem>
	 *            the generic type
	 * @param cls
	 *            the cls
	 * @return An ItemAttachment instance.
	 * @throws Exception
	 *             the exception
	 */
	public <TItem extends Item> GenericItemAttachment<TItem> addItemAttachment(
			Class<TItem> cls) throws Exception {
		if (cls.getDeclaredFields().length == 0) {
			throw new InvalidOperationException(String.format(
					"Items of type %s are not supported as attachments.", cls
							.getName()));
		}

		GenericItemAttachment<TItem> itemAttachment = 
				new GenericItemAttachment<TItem>(
				this.owner);
		itemAttachment.setTItem((TItem)EwsUtilities.createItemFromItemClass(
				itemAttachment, cls, true));

		this.internalAdd(itemAttachment);

		return itemAttachment;
	}

	/***
	 * Removes all attachments from this collection.
	 */
	public void clear() {
		this.internalClear();
	}

	/**
	 * Removes the attachment at the specified index.
	 * 
	 * @param index
	 *            Index of the attachment to remove.
	 */
	public void removeAt(int index) {
		if (index < 0 || index >= this.getCount()) {
			throw new IllegalArgumentException("parameter \'index\' : " +
					 Strings.IndexIsOutOfRange);
		}

		this.internalRemoveAt(index);
	}

	/**
	 * Removes the specified attachment.
	 * 
	 * @param attachment
	 *            The attachment to remove.
	 * @return True if the attachment was successfully removed from the
	 *         collection, false otherwise.
	 * @throws Exception
	 *             the exception
	 */
	public boolean remove(Attachment attachment) throws Exception {
		EwsUtilities.validateParam(attachment, "attachment");

		return this.internalRemove(attachment);
	}

	/**
	 * Instantiate the appropriate attachment type depending on the current XML
	 * element name.
	 * 
	 * @param xmlElementName
	 *            The XML element name from which to determine the type of
	 *            attachment to create.
	 * @return An Attachment instance.
	 */
	@Override
	protected Attachment createComplexProperty(String xmlElementName) {
		if (xmlElementName.equals(XmlElementNames.FileAttachment)) {
			return new FileAttachment(this.owner);
		} else if (xmlElementName.equals(XmlElementNames.ItemAttachment)) {
			return new ItemAttachment(this.owner);
		} else {
			return null;
		}
	}

	/**
	 * Determines the name of the XML element associated with the
	 * complexProperty parameter.
	 * 
	 * @param complexProperty
	 *            The attachment object for which to determine the XML element
	 *            name with.
	 * @return The XML element name associated with the complexProperty
	 *         parameter.
	 */
	@Override
	protected String getCollectionItemXmlElementName(Attachment 
			complexProperty) {
		if (complexProperty instanceof FileAttachment) {
			return XmlElementNames.FileAttachment;
		} else {
			return XmlElementNames.ItemAttachment;
		}
	}

	/**
	 * Saves this collection by creating new attachment and deleting removed
	 * ones.
	 * 
	 * @throws Exception
	 *             the exception
	 */
	protected void save() throws Exception {
		java.util.ArrayList<Attachment> attachments = 
			new java.util.ArrayList<Attachment>();

		for (Attachment attachment : this.getRemovedItems()) {
			if (!attachment.isNew()) {
				attachments.add(attachment);
			}
		}

		// If any, delete them by calling the DeleteAttachment web method.
		if (attachments.size() > 0) {
			this.internalDeleteAttachments(attachments);
		}
		
		attachments.clear();
		
		// Retrieve a list of attachments that have to be created.
		for (Attachment attachment : this) {
			if (attachment.isNew()) {
				attachments.add(attachment);
			}
		}

		// If there are any, create them by calling the CreateAttachment web
		// method.
		if (attachments.size() > 0) {
			if (this.owner.isAttachment()) {
				this.internalCreateAttachments(this.owner.getParentAttachment()
						.getId(), attachments);
			} else {
				this.internalCreateAttachments(
						this.owner.getId().getUniqueId(), attachments);
			}
		}

		
		// Process all of the item attachments in this collection.
		for (Attachment attachment : this) {
			ItemAttachment itemAttachment = (ItemAttachment)
				((attachment instanceof 
					ItemAttachment) ? attachment :
					 null);
			if (itemAttachment != null) {
				// Bug E14:80864: Make sure item was created/loaded before
				// trying to create/delete sub-attachments
				if (itemAttachment.getItem() != null) {
					// Create/delete any sub-attachments
					itemAttachment.getItem().getAttachments().save();

					// Clear the item's change log
					itemAttachment.getItem().clearChangeLog();
				}
			}
		}

		super.clearChangeLog();
	}

	/**
	 * Determines whether there are any unsaved attachment collection changes.
	 * @return True if attachment adds or deletes haven't been processed yet.
	 * @throws ServiceLocalException 
	 */
	protected boolean hasUnprocessedChanges() throws ServiceLocalException {
        // Any new attachments?
        for(Attachment attachment : this) {
            if (attachment.isNew()) {
                return true;
            }
        }

        // Any pending deletions?
        for(Attachment attachment : this.getRemovedItems()) {
            if (!attachment.isNew()) {
                return true;
            }
        }
      
        
    	Collection<ItemAttachment> itemAttachments = 
    		new ArrayList<ItemAttachment>();
    	for (Object event : this.getItems()) {
			if (event instanceof ItemAttachment) {
				itemAttachments.add((ItemAttachment)event);
			}
		}

        // Recurse: process item attachments to check 
    	// for new or deleted sub-attachments.
        for(ItemAttachment itemAttachment : itemAttachments) {
            if (itemAttachment.getItem() != null) {
                if (itemAttachment.getItem().getAttachments().hasUnprocessedChanges())
                {
                    return true;
                }
            }
        }

        return false;
    }
	
	/**
	 * Disables the change log clearing mechanism. Attachment collections are
	 * saved separately from the items they belong to.
	 */
	@Override
	protected void clearChangeLog() {
		// Do nothing
	}

	/**
	 * Validates this instance.
	 * 
	 * @throws Exception
	 *             the exception
	 */
	public void validate() throws Exception {
		// Validate all added attachments
		for (int attachmentIndex = 0; attachmentIndex < this.getAddedItems()
				.size(); attachmentIndex++) {
			Attachment attachment = this.getAddedItems().get(attachmentIndex);
			if (attachment.isNew()) {
				attachment.validate(attachmentIndex);
			}
		}
	}

	/**
	 * Calls the DeleteAttachment web method to delete a list of attachments.
	 * 
	 * @param attachments
	 *            the attachments
	 * @throws Exception
	 *             the exception
	 */
	private void internalDeleteAttachments(Iterable<Attachment> attachments)
			throws Exception {
		ServiceResponseCollection<DeleteAttachmentResponse> responses = 
			this.owner
				.getService().deleteAttachments(attachments);
		Enumeration<DeleteAttachmentResponse> enumerator = responses
				.getEnumerator();
		while (enumerator.hasMoreElements()) {
			DeleteAttachmentResponse response = enumerator.nextElement();
			// We remove all attachments that were successfully deleted from the
			// change log. We should never
			// receive a warning from EWS, so we ignore them.
			if (response.getResult() != ServiceResult.Error) {
				this.removeFromChangeLog(response.getAttachment());
			}
		}

		// TODO : Should we throw for warnings as well?
		if (responses.getOverallResult() == ServiceResult.Error) {
			throw new DeleteAttachmentException(responses,
					Strings.AtLeastOneAttachmentCouldNotBeDeleted);
		}
	}

	/**
	 * Calls the CreateAttachment web method to create a list of attachments.
	 * 
	 * @param parentItemId
	 *            the parent item id
	 * @param attachments
	 *            the attachments
	 * @throws Exception
	 *             the exception
	 */
	private void internalCreateAttachments(String parentItemId,
			Iterable<Attachment> attachments) throws Exception {
		ServiceResponseCollection<CreateAttachmentResponse> responses = 
			this.owner
				.getService().createAttachments(parentItemId, attachments);

		Enumeration<CreateAttachmentResponse> enumerator = responses
				.getEnumerator();
		while (enumerator.hasMoreElements()) {
			CreateAttachmentResponse response = enumerator.nextElement();
			// We remove all attachments that were successfully created from the
			// change log. We should never
			// receive a warning from EWS, so we ignore them.
			if (response.getResult() != ServiceResult.Error) {
				this.removeFromChangeLog(response.getAttachment());
			}
		}

		// TODO : Should we throw for warnings as well?
		if (responses.getOverallResult() == ServiceResult.Error) {
			throw new CreateAttachmentException(responses,
					Strings.AttachmentCreationFailed);
		}
	}

}
