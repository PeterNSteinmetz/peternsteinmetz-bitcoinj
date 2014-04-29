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
import com.google.bitcoin.core.StoredTransactionOutput;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.annotation.Nullable;
import java.math.BigInteger;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
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

  @Nullable
  public StoredTransactionOutput getTransactionOutput(Sha256Hash hash, long index) throws BlockStoreException {
    maybeConnect();
    PreparedStatement s = null;
    try {
      s = conn.get()
         .prepareStatement("SELECT height, value, scriptBytes FROM openOutputs " +
            "WHERE hash = ? AND index = ?");
      s.setBytes(1, hash.getBytes());
      // index is actually an unsigned int
      s.setInt(2, (int)index);
      ResultSet results = s.executeQuery();
      if (!results.next()) {
        return null;
      }
      // Parse it.
      int height = results.getInt(1);
      BigInteger value = new BigInteger(results.getBytes(2));
      // Tell the StoredTransactionOutput that we are a coinbase, as that is encoded in height
      StoredTransactionOutput txout = new StoredTransactionOutput(hash, index, value, height, true, results.getBytes(3));
      return txout;
    } catch (SQLException ex) {
      throw new BlockStoreException(ex);
    } finally {
      if (s != null)
        try {
          s.close();
        } catch (SQLException e) { throw new BlockStoreException("Failed to close PreparedStatement"); }
    }
  }

  public boolean hasUnspentOutputs(Sha256Hash hash, int numOutputs) throws BlockStoreException {
    maybeConnect();
    PreparedStatement s = null;
    try {
      s = conn.get()
         .prepareStatement("SELECT COUNT(*) FROM openOutputs WHERE hash = ?");
      s.setBytes(1, hash.getBytes());
      ResultSet results = s.executeQuery();
      if (!results.next()) {
        throw new BlockStoreException("Got no results from a COUNT(*) query");
      }
      int count = results.getInt(1);
      return count != 0;
    } catch (SQLException ex) {
      throw new BlockStoreException(ex);
    } finally {
      if (s != null)
        try {
          s.close();
        } catch (SQLException e) { throw new BlockStoreException("Failed to close PreparedStatement"); }
    }
  }

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
