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
import com.spectralogic.ds3cli.View;
import com.spectralogic.ds3cli.ViewType;
import com.spectralogic.ds3cli.exceptions.CommandException;
import com.spectralogic.ds3cli.models.GetUsersResult;
import com.spectralogic.ds3cli.util.Ds3Provider;
import com.spectralogic.ds3cli.util.FileUtils;
import com.spectralogic.ds3client.commands.spectrads3.GetUsersSpectraS3Request;
import com.spectralogic.ds3client.commands.spectrads3.GetUsersSpectraS3Response;
import com.spectralogic.ds3client.networking.FailedRequestException;
import com.spectralogic.ds3client.utils.SSLSetupException;

import java.io.IOException;
import java.security.SignatureException;

public class GetUsers extends CliCommand<GetUsersResult> {

    public GetUsers() {
    }

    protected final View<GetUsersResult> cliView = new com.spectralogic.ds3cli.views.cli.GetUsersView();
    protected final View<GetUsersResult> jsonView = new com.spectralogic.ds3cli.views.json.GetUsersView();

    @Override
    public CliCommand init(final Arguments args) throws Exception {
        return this;
    }

    @Override
    public GetUsersResult call() throws IOException, SignatureException, SSLSetupException, CommandException {
        try {
            final GetUsersSpectraS3Response response = getClient().getUsersSpectraS3(new GetUsersSpectraS3Request());

            return new GetUsersResult(response.getSpectraUserListResult());
        } catch (final FailedRequestException e) {
            throw new CommandException("Failed to Get Users", e);
        }
    }

    @Override
    public View getView(final ViewType viewType) {
        if (viewType == ViewType.JSON) {
            return this.jsonView;
        }
        return this.cliView;
    }
}
