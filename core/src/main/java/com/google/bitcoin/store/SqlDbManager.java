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

import java.sql.Connection;

/**
 * <p>Interface for managing common operations, such as transaction batching and
 * connection maintainence for a SQL database.</p>
 *
 * <p>In general, different databases will provide different implementations. </p>
 */
public interface SqlDbManager {

  /**
   * <p>Test for an active connection and obtain one if needed.</p>
   */
  public void maybeConnect() throws BlockStoreException;

  /**
   * <p>Return the currently active connection.</p>
   */
  public ThreadLocal<Connection> getConnection() throws BlockStoreException;


}
