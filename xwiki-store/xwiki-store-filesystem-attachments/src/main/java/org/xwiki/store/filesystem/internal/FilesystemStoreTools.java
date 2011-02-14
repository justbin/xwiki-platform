/*
 * See the NOTICE file distributed with this work for additional
 * information regarding copyright ownership.
 *
 * This is free software; you can redistribute it and/or modify it
 * under the terms of the GNU Lesser General Public License as
 * published by the Free Software Foundation; either version 2.1 of
 * the License, or (at your option) any later version.
 *
 * This software is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with this software; if not, write to the Free
 * Software Foundation, Inc., 51 Franklin St, Fifth Floor, Boston, MA
 * 02110-1301 USA, or see the FSF site: http://www.fsf.org.
 */
package org.xwiki.store.filesystem.internal;

import java.io.File;
import java.util.Date;
import java.util.concurrent.locks.ReadWriteLock;

import com.xpn.xwiki.doc.XWikiAttachment;
import org.xwiki.component.annotation.ComponentRole;


/**
 * Tools for getting files to store data in the filesystem.
 * This should be replaced by a module which provides a secure extension of java.io.File.
 *
 * @version $Id$
 * @since 3.0M2
 */
@ComponentRole
public interface FilesystemStoreTools
{
    /**
     * Get a backup file which for a given storage file.
     * This file name will never collide with any other file gotten through this interface.
     *
     * @param storageFile the file to get a backup file for.
     * @return a backup file with a name based on the name of the given file.
     */
    File getBackupFile(final File storageFile);

    /**
     * Get a temporary file which for a given storage file.
     * This file name will never collide with any other file gotten through this interface.
     *
     * @param storageFile the file to get a temporary file for.
     * @return a temporary file with a name based on the name of the given file.
     */
    File getTempFile(final File storageFile);

    /**
     * Get an instance of AttachmentFileProvider which will save everything to do with an attachment
     * in a separate location which is repeatable only with the same attachment name, and containing
     * document.
     *
     * @param attachment the attachment to get a tools for.
     * @return a provider which will provide files with collision free path and repeatable with same inputs.
     */
    AttachmentFileProvider getAttachmentFileProvider(final XWikiAttachment attachment);

    /**
     * Get an instance of AttachmentFileProvider which will save everything to do with an attachment
     * in a separate location which is repeatable only with the same attachment name, containing document,
     * and date of deletion.
     *
     * @param attachment the attachment to get a tools for.
     * @param deleteDate the date the attachment was deleted.
     * @return a provider which will provide files with collision free path and repeatable with same inputs.
     */
    AttachmentFileProvider getDeletedAttachmentFileProvider(final XWikiAttachment attachment,
                                                            final Date deleteDate);

    /**
     * Get a {@link java.util.concurrent.locks.ReadWriteLock} which is unique to the given file.
     * This method will always return the same lock for the path on the filesystem even if the 
     * {@link java.io.File} object is different.
     *
     * @param toLock the file to get a lock for.
     * @return a lock for the given file.
     */
    ReadWriteLock getLockForFile(final File toLock);
}