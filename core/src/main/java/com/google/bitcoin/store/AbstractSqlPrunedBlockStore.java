/*
 * Copyright 2014 Peter N. Steinmetz
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.google.bitcoin.store;

import com.google.bitcoin.core.NetworkParameters;
import com.google.bitcoin.core.Sha256Hash;
import com.google.bitcoin.core.StoredBlock;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.util.List;

/**
 * <p>Provides an abstract base class for {@link com.google.bitcoin.store.PrunedBlockStore}s
 * which use an SQL database for storage.</p>
 */
public abstract class AbstractSqlPrunedBlockStore implements PrunedBlockStore {
  private static final Logger log = LoggerFactory.getLogger(AbstractSqlPrunedBlockStore.class);

  protected Sha256Hash chainHeadHash;
  protected StoredBlock chainHeadBlock;
  protected Sha256Hash verifiedChainHeadHash;
  protected StoredBlock verifiedChainHeadBlock;
  protected NetworkParameters params;
  protected ThreadLocal<Connection> conn;
  protected List<Connection> allConnections;
  protected String connectionURL;
  protected int fullStoreDepth;

  // :TODO: chainhead settings should be maintained by separate object
  protected static final String CHAIN_HEAD_SETTING = "chainhead";
  protected static final String VERIFIED_CHAIN_HEAD_SETTING = "verifiedchainhead";
  protected static final String VERSION_SETTING = "version";


  /**
   * <p>Establish a connection, if one doesn't already exist.</p>
   *
   * <p>Implementations should be synchronized for thread safety.</p>
   *
   * @throws BlockStoreException
   */
  protected abstract void maybeConnect() throws BlockStoreException;

  @Override
  public StoredBlock getChainHead() throws BlockStoreException {
    return chainHeadBlock;
  }

  @Override
  public void setChainHead(StoredBlock chainHead) throws BlockStoreException {
    Sha256Hash hash = chainHead.getHeader().getHash();
    this.chainHeadHash = hash;
    this.chainHeadBlock = chainHead;
    maybeConnect();
    try {
      PreparedStatement s = conn.get()
         .prepareStatement("UPDATE settings SET value = ? WHERE name = ?");
      s.setString(2, CHAIN_HEAD_SETTING);
      s.setBytes(1, hash.getBytes());
      s.executeUpdate();
      s.close();
    } catch (SQLException ex) {
      throw new BlockStoreException(ex);
    }
  }

  @Override
  public StoredBlock getVerifiedChainHead() throws BlockStoreException {
    return verifiedChainHeadBlock;
  }

  @Override
  public void setVerifiedChainHead(StoredBlock chainHead) throws BlockStoreException {
    Sha256Hash hash = chainHead.getHeader().getHash();
    this.verifiedChainHeadHash = hash;
    this.verifiedChainHeadBlock = chainHead;
    maybeConnect();
    try {
      PreparedStatement s = conn.get()
         .prepareStatement("UPDATE settings SET value = ? WHERE name = ?");
      s.setString(2, VERIFIED_CHAIN_HEAD_SETTING);
      s.setBytes(1, hash.getBytes());
      s.executeUpdate();
      s.close();
    } catch (SQLException ex) {
      throw new BlockStoreException(ex);
    }
    if (this.chainHeadBlock.getHeight() < chainHead.getHeight())
      setChainHead(chainHead);
    removeUndoableBlocksWhereHeightIsLessThan(chainHead.getHeight() - fullStoreDepth);
  }

  private void removeUndoableBlocksWhereHeightIsLessThan(int height) throws BlockStoreException {
    try {
      PreparedStatement s = conn.get()
         .prepareStatement("DELETE FROM undoableBlocks WHERE height <= ?");
      s.setInt(1, height);

      if (log.isDebugEnabled())
        log.debug("Deleting undoable undoable block with height <= " + height);


      s.executeUpdate();
      s.close();
    } catch (SQLException ex) {
      throw new BlockStoreException(ex);
    }
  }


  /**
   * <p>Begins/Commits/Aborts a database transaction.</p>
   *
   * <p>If abortDatabaseBatchWrite() is called by the same thread that called beginDatabaseBatchWrite(),
   * any data writes between this call and abortDatabaseBatchWrite() made by the same thread
   * should be discarded.</p>
   *
   * <p>Furthermore, any data written after a call to beginDatabaseBatchWrite() should not be readable
   * by any other threads until commitDatabaseBatchWrite() has been called by this thread.
   * Multiple calls to beginDatabaseBatchWrite() in any given thread should be ignored and treated as one call.</p>
   */
  @Override
  public void beginBatchWrite() throws BlockStoreException {

    maybeConnect();
    if (log.isDebugEnabled())
      log.debug("Starting database batch write with connection: " + conn.get().toString());

    try {
      conn.get().setAutoCommit(false);
    } catch (SQLException e) {
      throw new BlockStoreException(e);
    }
  }

  @Override
  public void commitBatchWrite() throws BlockStoreException {
    maybeConnect();

    if (log.isDebugEnabled())
      log.debug("Committing database batch write with connection: " + conn.get().toString());

    try {
      conn.get().commit();
      conn.get().setAutoCommit(true);
    } catch (SQLException e) {
      throw new BlockStoreException(e);
    }
  }

  @Override
  public void abortBatchWrite() throws BlockStoreException {

    maybeConnect();
    if (log.isDebugEnabled())
      log.debug("Rollback database batch write with connection: " + conn.get().toString());

    try {
      if (!conn.get().getAutoCommit()) {
        conn.get().rollback();
        conn.get().setAutoCommit(true);
      } else {
        log.warn("Warning: Rollback attempt without transaction");
      }
    } catch (SQLException e) {
      throw new BlockStoreException(e);
    }
  }

}
