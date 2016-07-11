/*
 * ******************************************************************************
 *   Copyright 2014 Spectra Logic Corporation. All Rights Reserved.
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
import com.spectralogic.ds3cli.models.GetPhysicalPlacementWithFullDetailsResult;
import com.spectralogic.ds3cli.util.Ds3Provider;
import com.spectralogic.ds3cli.util.FileUtils;
import com.spectralogic.ds3client.commands.spectrads3.GetPhysicalPlacementForObjectsWithFullDetailsSpectraS3Request;
import com.spectralogic.ds3client.commands.spectrads3.GetPhysicalPlacementForObjectsWithFullDetailsSpectraS3Response;
import com.spectralogic.ds3client.models.bulk.Ds3Object;
import com.spectralogic.ds3client.networking.FailedRequestException;
import org.apache.commons.cli.MissingOptionException;
import org.apache.commons.cli.UnrecognizedOptionException;

import java.util.Arrays;
import java.util.List;


public class GetPhysicalPlacement extends CliCommand<GetPhysicalPlacementWithFullDetailsResult> {

    private String bucketName;
    private String objectName;

    protected final View<GetPhysicalPlacementWithFullDetailsResult> cliView = new com.spectralogic.ds3cli.views.cli.GetPhysicalPlacementWithFullDetailsView();
    protected final View<GetPhysicalPlacementWithFullDetailsResult> jsonView = new com.spectralogic.ds3cli.views.json.GetPhysicalPlacementWithFullDetailsView();

    public GetPhysicalPlacement() {
    }


    @Override
    public CliCommand init(final Arguments args) throws Exception {
        if (args.getDirectory() != null) {
            throw new UnrecognizedOptionException("The get_physical_placement command does not work with '-d' as it only applies to a single object.");
        }

        this.bucketName = args.getBucket();
        if (this.bucketName == null) {
            throw new MissingOptionException("The get_physical_placement command requires '-b' to be set.");
        }

        this.objectName = args.getObjectName();
        if (this.objectName == null) {
            throw new MissingOptionException("The get_physical_placement command requires '-o' to be set.");
        }

        return this;
    }

    @Override
    public GetPhysicalPlacementWithFullDetailsResult call() throws Exception {
        try {
            final List<Ds3Object> objectsList = Arrays.asList(new Ds3Object(objectName));

            final GetPhysicalPlacementForObjectsWithFullDetailsSpectraS3Response response = getClient().
                    getPhysicalPlacementForObjectsWithFullDetailsSpectraS3(
                            new GetPhysicalPlacementForObjectsWithFullDetailsSpectraS3Request(bucketName, objectsList));

            return new GetPhysicalPlacementWithFullDetailsResult(response.getBulkObjectListResult());
        } catch (final FailedRequestException e) {
            throw new CommandException("Failed Get Physical Placement", e);
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
