/**
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements. See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License. You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.apache.activemq.tests.integration.cluster.failover;

import java.util.HashSet;
import java.util.Set;

import org.apache.activemq.api.core.TransportConfiguration;
import org.apache.activemq.api.core.client.ClientSession;
import org.apache.activemq.api.core.client.ClientSessionFactory;
import org.apache.activemq.core.config.ha.SharedStoreMasterPolicyConfiguration;
import org.apache.activemq.core.config.ha.SharedStoreSlavePolicyConfiguration;
import org.apache.activemq.core.security.Role;
import org.apache.activemq.core.server.impl.InVMNodeManager;
import org.apache.activemq.spi.core.security.ActiveMQSecurityManager;
import org.apache.activemq.tests.integration.cluster.util.TestableServer;

/**
 * A SecurityFailoverTest
 *
 * @author clebertsuconic
 *
 *
 */
public class SecurityFailoverTest extends FailoverTest
{

   protected ClientSession createSession(ClientSessionFactory sf,
                                         boolean isXA,
                                         boolean autoCommitSends,
                                         boolean autoCommitAcks,
                                         int ackBatchSize) throws Exception
   {
      ClientSession session =
               sf.createSession("a",
                              "b",
                              isXA,
                              autoCommitSends,
                              autoCommitAcks,
                              sf.getServerLocator().isPreAcknowledge(),
                              ackBatchSize);
      addClientSession(session);
      return session;
   }

   @Override
   protected ClientSession createSession(ClientSessionFactory sf,
                                         boolean autoCommitSends,
                                         boolean autoCommitAcks,
                                         int ackBatchSize) throws Exception
   {
      ClientSession session =
               sf.createSession("a", "b", false, autoCommitSends, autoCommitAcks, sf.getServerLocator()
                                                                                  .isPreAcknowledge(), ackBatchSize);
      addClientSession(session);
      return session;
   }

   @Override
   protected ClientSession createSession(ClientSessionFactory sf, boolean autoCommitSends, boolean autoCommitAcks) throws Exception
   {
      return createSession(sf, autoCommitSends, autoCommitAcks, sf.getServerLocator().getAckBatchSize());
   }

   @Override
   protected ClientSession createSession(ClientSessionFactory sf) throws Exception
   {
      return createSession(sf, true, true, sf.getServerLocator().getAckBatchSize());
   }

   @Override
   protected ClientSession createSession(ClientSessionFactory sf,
                                         boolean xa,
                                         boolean autoCommitSends,
                                         boolean autoCommitAcks) throws Exception
   {
      return createSession(sf, xa, autoCommitSends, autoCommitAcks, sf.getServerLocator().getAckBatchSize());
   }

   /**
    * @throws Exception
    */
   @Override
   protected void createConfigs() throws Exception
   {
      nodeManager = new InVMNodeManager(false);
      TransportConfiguration liveConnector = getConnectorTransportConfiguration(true);
      TransportConfiguration backupConnector = getConnectorTransportConfiguration(false);

      backupConfig = super.createDefaultConfig()
         .clearAcceptorConfigurations()
         .addAcceptorConfiguration(getAcceptorTransportConfiguration(false))
         .setSecurityEnabled(true)
         .setHAPolicyConfiguration(new SharedStoreSlavePolicyConfiguration()
                                      .setFailbackDelay(1000))
         .addConnectorConfiguration(liveConnector.getName(), liveConnector)
         .addConnectorConfiguration(backupConnector.getName(), backupConnector)
         .addClusterConfiguration(basicClusterConnectionConfig(backupConnector.getName(), liveConnector.getName()));

      backupServer = createTestableServer(backupConfig);
      ActiveMQSecurityManager securityManager = installSecurity(backupServer);
      securityManager.setDefaultUser(null);

      liveConfig = super.createDefaultConfig()
         .clearAcceptorConfigurations()
         .addAcceptorConfiguration(getAcceptorTransportConfiguration(true))
         .setSecurityEnabled(true)
         .setHAPolicyConfiguration(new SharedStoreMasterPolicyConfiguration())
         .addClusterConfiguration(basicClusterConnectionConfig(liveConnector.getName()))
         .addConnectorConfiguration(liveConnector.getName(), liveConnector);

      liveServer = createTestableServer(liveConfig);
      installSecurity(liveServer);
   }

   @Override
   protected void beforeRestart(TestableServer server)
   {
      installSecurity(server);
   }


   /**
    * @return
    */
   protected ActiveMQSecurityManager installSecurity(TestableServer server)
   {
      ActiveMQSecurityManager securityManager = server.getServer().getSecurityManager();
      securityManager.addUser("a", "b");
      Role role = new Role("arole", true, true, true, true, true, true, true);
      Set<Role> roles = new HashSet<Role>();
      roles.add(role);
      server.getServer().getSecurityRepository().addMatch("#", roles);
      securityManager.addRole("a", "arole");
      return securityManager;
   }
}