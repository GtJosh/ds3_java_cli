/*
 * ******************************************************************************
 *   Copyright 2016 Spectra Logic Corporation. All Rights Reserved.
 *   Licensed under the Apache License, Version 2.0 (the "License"). You may not use
 *   this file except in compliance with the License. A copy of the License is located at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 *   or in the "license" file accompanying this file.
 *   This file is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR
 *   CONDITIONS OF ANY KIND, either express or implied. See the License for the
 *   specific language governing permissions and limitations under the License.
 * ****************************************************************************
 */

package com.spectralogic.ds3cli.command;

import com.spectralogic.ds3cli.Arguments;
import com.spectralogic.ds3cli.exceptions.CommandException;
import com.spectralogic.ds3cli.models.GetDataPoliciesResult;
import com.spectralogic.ds3cli.util.Ds3Provider;
import com.spectralogic.ds3cli.util.FileUtils;
import com.spectralogic.ds3client.commands.spectrads3.GetDataPolicySpectraS3Request;
import com.spectralogic.ds3client.commands.spectrads3.GetDataPolicySpectraS3Response;
import com.spectralogic.ds3client.networking.FailedRequestException;
import com.spectralogic.ds3client.utils.SSLSetupException;
import org.apache.commons.cli.MissingOptionException;

import java.io.IOException;
import java.security.SignatureException;

public class GetDataPolicy extends CliCommand<GetDataPoliciesResult> {

    // name or guid
    private String policyId;

    public GetDataPolicy(final Ds3Provider provider, final FileUtils fileUtils) {
        super(provider, fileUtils);
    }

    @Override
    public CliCommand init(final Arguments args) throws Exception {
        this.policyId = args.getId();
        if (this.policyId == null) {
            throw new MissingOptionException("The get policy command requires '-i' to be set with the policy name or Id");
        }
        return this;
    }

    @Override
    public String getLongHelp() {
        final StringBuffer helpStringBuffer = new StringBuffer();
        helpStringBuffer.append("Returns information about the specified data policy..\n");
        helpStringBuffer.append("Requires the '-i' parameter to specify data policy (UUID or name).");
        helpStringBuffer.append("\nUse the get_data_policies command to retrieve a list of policies");

        return helpStringBuffer.toString();
    }

    @Override
    public GetDataPoliciesResult call() throws IOException, SignatureException, SSLSetupException, CommandException {
        try {
            final GetDataPolicySpectraS3Response response = getClient().getDataPolicySpectraS3(new GetDataPolicySpectraS3Request(this.policyId));

            return new GetDataPoliciesResult(response.getDataPolicyResult());
        } catch (final FailedRequestException e) {
            throw new CommandException("Failed Get Data Policies", e);
        }
    }
}
