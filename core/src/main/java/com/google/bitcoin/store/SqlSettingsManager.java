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

import com.google.bitcoin.core.Sha256Hash;
import com.google.bitcoin.core.StoredBlock;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;

/**
 * <p>Provides management of store settings, such as chain heads, in a SQL database.</p>
 */
public class SqlSettingsManager {
  private static final Logger log = LoggerFactory.getLogger(SqlSettingsManager.class);

  protected Sha256Hash chainHeadHash;
  protected Sha256Hash verifiedChainHeadHash;

  protected ThreadLocal<Connection> conn;

  protected static final String CHAIN_HEAD_SETTING = "chainhead";
  protected static final String VERIFIED_CHAIN_HEAD_SETTING = "verifiedchainhead";
  protected static final String VERSION_SETTING = "version";

  public void setChainHeadHash(Sha256Hash hash) {
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
  public Sha256Hash getChainHeadHash() throws BlockStoreException {
    return chainHeadHash;
  }

}
