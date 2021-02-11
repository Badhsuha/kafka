/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements. See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License. You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.apache.kafka.controller;

import org.apache.kafka.clients.admin.AlterConfigOp;
import org.apache.kafka.common.config.ConfigResource;
import org.apache.kafka.common.message.AlterIsrRequestData;
import org.apache.kafka.common.message.AlterIsrResponseData;
import org.apache.kafka.common.message.BrokerHeartbeatRequestData;
import org.apache.kafka.common.message.BrokerRegistrationRequestData;
import org.apache.kafka.common.message.CreateTopicsRequestData;
import org.apache.kafka.common.message.CreateTopicsResponseData;
import org.apache.kafka.common.message.ElectLeadersRequestData;
import org.apache.kafka.common.message.ElectLeadersResponseData;
import org.apache.kafka.common.quota.ClientQuotaAlteration;
import org.apache.kafka.common.quota.ClientQuotaEntity;
import org.apache.kafka.common.requests.ApiError;
import org.apache.kafka.metadata.BrokerHeartbeatReply;
import org.apache.kafka.metadata.BrokerRegistrationReply;
import org.apache.kafka.metadata.FeatureMapAndEpoch;

import java.util.Collection;
import java.util.Map;
import java.util.concurrent.CompletableFuture;


public interface Controller extends AutoCloseable {
    /**
     * Change the in-sync replica sets for some partitions.
     *
     * @param request       The AlterIsrRequest data.
     *
     * @return              A future yielding the response.
     */
    CompletableFuture<AlterIsrResponseData> alterIsr(AlterIsrRequestData request);

    /**
     * Create a batch of topics.
     *
     * @param request       The CreateTopicsRequest data.
     *
     * @return              A future yielding the response.
     */
    CompletableFuture<CreateTopicsResponseData>
        createTopics(CreateTopicsRequestData request);

    /**
     * Unregister a broker.
     *
     * @param brokerId      The broker id to unregister.
     *
     * @return              A future that is completed successfully when the broker is
     *                      unregistered.
     */
    CompletableFuture<Void> unregisterBroker(int brokerId);

    /**
     * Describe the current configuration of various resources.
     *
     * @param resources     A map from resources to the collection of config keys that we
     *                      want to describe for each.  If the collection is empty, then
     *                      all configuration keys will be described.
     *
     * @return
     */
    CompletableFuture<Map<ConfigResource, ResultOrError<Map<String, String>>>>
        describeConfigs(Map<ConfigResource, Collection<String>> resources);

    /**
     * Elect new partition leaders.
     *
     * @param request       The request.
     *
     * @return              A future yielding the elect leaders response.
     */
    CompletableFuture<ElectLeadersResponseData> electLeaders(ElectLeadersRequestData request);

    /**
     * Get the current finalized feature ranges for each feature.
     *
     * @return              A future yielding the feature ranges.
     */
    CompletableFuture<FeatureMapAndEpoch> finalizedFeatures();

    /**
     * Perform some incremental configuration changes.
     *
     * @param configChanges The changes.
     * @param validateOnly  True if we should validate the changes but not apply them.
     *
     * @return              A future yielding a map from partitions to error results.
     */
    CompletableFuture<Map<ConfigResource, ApiError>> incrementalAlterConfigs(
        Map<ConfigResource, Map<String, Map.Entry<AlterConfigOp.OpType, String>>> configChanges,
        boolean validateOnly);

    /**
     * Perform some configuration changes using the legacy API.
     *
     * @param newConfigs    The new configuration maps to apply.
     * @param validateOnly  True if we should validate the changes but not apply them.
     *
     * @return              A future yielding a map from partitions to error results.
     */
    CompletableFuture<Map<ConfigResource, ApiError>> legacyAlterConfigs(
        Map<ConfigResource, Map<String, String>> newConfigs, boolean validateOnly);

    /**
     * Process a heartbeat from a broker.
     *
     * @param request      The broker heartbeat request.
     *
     * @return              A future yielding a heartbeat reply.
     */
    CompletableFuture<BrokerHeartbeatReply> processBrokerHeartbeat(
        BrokerHeartbeatRequestData request);

    /**
     * Attempt to register the given broker.
     *
     * @param request      The registration request.
     *
     * @return              A future yielding a registration reply.
     */
    CompletableFuture<BrokerRegistrationReply> registerBroker(
        BrokerRegistrationRequestData request);

    /**
     * Wait for the given number of brokers to be registered and unfenced.
     * This is for testing.
     *
     * @param minBrokers    The minimum number of brokers to wait for.
     * @return              A future which is completed when the given number of brokers
     *                      is reached.
     */
    CompletableFuture<Void> waitForReadyBrokers(int minBrokers);

    /**
     * Perform some client quota changes
     *
     * @param quotaAlterations The list of quotas to alter
     * @param validateOnly     True if we should validate the changes but not apply them.
     * @return                 A future yielding a map of quota entities to error results.
     */
    CompletableFuture<Map<ClientQuotaEntity, ApiError>> alterClientQuotas(
        Collection<ClientQuotaAlteration> quotaAlterations, boolean validateOnly
    );

    /**
     * Begin shutting down, but don't block.  You must still call close to clean up all
     * resources.
     */
    void beginShutdown();

    /**
     * If this controller is active, this is the non-negative controller epoch.
     * Otherwise, this is -1.
     */
    long curClaimEpoch();

    /**
     * Blocks until we have shut down and freed all resources.
     */
    void close() throws InterruptedException;
}
