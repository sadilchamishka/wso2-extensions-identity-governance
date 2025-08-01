/*
 * Copyright (c) 2025, WSO2 LLC. (https://www.wso2.com) All Rights Reserved.
 *
 * WSO2 LLC. licenses this file to you under the Apache License,
 * Version 2.0 (the "License"); you may not use this file except
 * in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied. See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */

package org.wso2.carbon.identity.recovery.executor;

import org.apache.commons.lang.StringUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.wso2.carbon.identity.application.common.model.User;
import org.wso2.carbon.identity.core.util.IdentityTenantUtil;
import org.wso2.carbon.identity.flow.execution.engine.Constants;
import org.wso2.carbon.identity.flow.execution.engine.exception.FlowEngineException;
import org.wso2.carbon.identity.flow.execution.engine.graph.Executor;
import org.wso2.carbon.identity.flow.execution.engine.model.ExecutorResponse;
import org.wso2.carbon.identity.flow.execution.engine.model.FlowExecutionContext;
import org.wso2.carbon.identity.recovery.IdentityRecoveryException;
import org.wso2.carbon.identity.recovery.internal.IdentityRecoveryServiceDataHolder;
import org.wso2.carbon.identity.recovery.password.NotificationPasswordRecoveryManager;
import org.wso2.carbon.user.api.UserStoreException;
import org.wso2.carbon.user.api.UserStoreManager;
import org.wso2.carbon.user.core.common.AbstractUserStoreManager;

import java.util.Arrays;
import java.util.List;

import static org.wso2.carbon.identity.flow.execution.engine.Constants.ExecutorStatus.STATUS_COMPLETE;

/**
 * Validates the confirmation code provided by the user during the ask password flow.
 */
public class ConfirmationCodeValidationExecutor implements Executor {

    private static final Log LOG = LogFactory.getLog(ConfirmationCodeValidationExecutor.class);
    private static final String CONFIRMATION_CODE = "confirmationCode";

    @Override
    public String getName() {

        return "ConfirmationCodeValidationExecutor";
    }

    @Override
    public ExecutorResponse execute(FlowExecutionContext flowExecutionContext) {

        ExecutorResponse response = new ExecutorResponse();
        String confirmationCode = flowExecutionContext.getUserInputData().get(CONFIRMATION_CODE);
        if (StringUtils.isBlank(confirmationCode)) {
            return clientInputRequiredResponse(response, CONFIRMATION_CODE);
        }
        try {
            NotificationPasswordRecoveryManager notificationPasswordRecoveryManager = NotificationPasswordRecoveryManager
                    .getInstance();
            User user = notificationPasswordRecoveryManager.
                    getValidatedUser(confirmationCode, null);
            int tenantId = IdentityTenantUtil.getTenantId(user.getTenantDomain());
            UserStoreManager userStoreManager = IdentityRecoveryServiceDataHolder.getInstance().
                    getRealmService().getTenantUserRealm(tenantId).getUserStoreManager();
            String userid = ((AbstractUserStoreManager) userStoreManager).getUserIDFromUserName(user.getUserName());
            flowExecutionContext.getFlowUser().setUserId(userid);
            flowExecutionContext.getFlowUser().setUsername(user.getUserName());
            flowExecutionContext.setProperty(CONFIRMATION_CODE, confirmationCode);
            response.setResult(STATUS_COMPLETE);
            return response;
        } catch (IdentityRecoveryException | UserStoreException e) {
            String errorMessage = "Error while validating the confirmation code.";
            LOG.error(errorMessage, e);
            return errorResponse(response, errorMessage);
        }
    }

    @Override
    public List<String> getInitiationData() {

        return null;
    }

    @Override
    public ExecutorResponse rollback(FlowExecutionContext flowExecutionContext) {

        return null;
    }

    /**
     * Creates an error response with the provided message.
     *
     * @param response ExecutorResponse to be modified with error details.
     * @param message  Error message to be set in the response.
     * @return Modified ExecutorResponse with error details.
     */
    private ExecutorResponse errorResponse(ExecutorResponse response, String message) {

        response.setErrorMessage(message);
        response.setResult(Constants.ExecutorStatus.STATUS_ERROR);
        return response;
    }

    /**
     * Creates a response indicating that client input is required.
     *
     * @param response ExecutorResponse to be modified.
     * @param fields   Fields that are required from the client.
     * @return Modified ExecutorResponse indicating client input is required.
     */
    private ExecutorResponse clientInputRequiredResponse(ExecutorResponse response, String... fields) {

        response.setResult(Constants.ExecutorStatus.STATUS_CLIENT_INPUT_REQUIRED);
        response.setRequiredData(Arrays.asList(fields));
        return response;
    }
}
