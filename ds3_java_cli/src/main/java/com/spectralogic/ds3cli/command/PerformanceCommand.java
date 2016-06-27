package com.spectralogic.ds3cli.command;

import com.google.common.collect.Lists;
import com.spectralogic.ds3cli.Arguments;
import com.spectralogic.ds3cli.ViewType;
import com.spectralogic.ds3cli.exceptions.CommandException;
import com.spectralogic.ds3cli.models.PerformanceResult;
import com.spectralogic.ds3cli.util.Ds3Provider;
import com.spectralogic.ds3cli.util.FileUtils;
import com.spectralogic.ds3cli.util.MemoryObjectChannelBuilder;
import com.spectralogic.ds3client.Ds3Client;
import com.spectralogic.ds3client.commands.DeleteBucketRequest;
import com.spectralogic.ds3client.commands.DeleteObjectRequest;
import com.spectralogic.ds3client.commands.PutBucketRequest;
import com.spectralogic.ds3client.helpers.DataTransferredListener;
import com.spectralogic.ds3client.helpers.Ds3ClientHelpers;
import com.spectralogic.ds3client.helpers.ObjectCompletedListener;
import com.spectralogic.ds3client.models.Contents;
import com.spectralogic.ds3client.models.bulk.Ds3Object;
import com.spectralogic.ds3client.networking.FailedRequestException;
import com.spectralogic.ds3client.serializer.XmlProcessingException;
import org.apache.commons.cli.MissingOptionException;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.SeekableByteChannel;
import java.security.SignatureException;
import java.util.List;


public class PerformanceCommand extends CliCommand<PerformanceResult> {

    private String bucketName;
    private String numberOfFiles;
    private String sizeOfFiles;
    private int bufferSize;
    private int numberOfThreads;
    private boolean doNotDelete;

    public PerformanceCommand(final Ds3Provider provider, final FileUtils fileUtils) {
        super(provider, fileUtils);
    }

    @Override
    public CliCommand init(final Arguments args) throws Exception {
        if (args.getOutputFormat() == ViewType.JSON) {
            throw new CommandException("Json output is not supported with the performance command");
        }

        bucketName = args.getBucket();
        if (bucketName == null) {
            throw new MissingOptionException("The performance command requires '-b' to be set.");
        }

        numberOfFiles = args.getNumberOfFiles();
        if (numberOfFiles == null) {
            throw new MissingOptionException("The performance command requires '-n' to be set.");
        }

        sizeOfFiles = args.getSizeOfFiles();
        if (sizeOfFiles == null) {
            throw new MissingOptionException("The performance command requires '-s' to be set.");
        }

        bufferSize = Integer.valueOf(args.getBufferSize());
        this.numberOfThreads = Integer.valueOf(args.getNumberOfThreads());

        return this;
    }

    @Override
    public String getLongHelp() {
        final StringBuffer helpStringBuffer = new StringBuffer();
        helpStringBuffer.append("For internal testing.\n");
        helpStringBuffer.append("Generates mock file streams for put, and a discard (/dev/null)\n");
        helpStringBuffer.append("stream for get. Useful for testing network and system performance.\n");
        helpStringBuffer.append("Requires the '-b' parameter with a unique bucketname to be used for the test.\n");
        helpStringBuffer.append("Requires the '-n' parameter with the number of files to be used for the test.\n");
        helpStringBuffer.append("Requires the '-s' parameter with the size of each file in MB for the test.\n");
        helpStringBuffer.append("Optional '-bs' parameter with the buffer size in bytes (default 1MB).\n");
        helpStringBuffer.append("Optional '-nt' parameter with the number of threads.\n");

        return helpStringBuffer.toString();
    }



    @Override
    public PerformanceResult call() throws Exception {
        final Ds3ClientHelpers helpers = Ds3ClientHelpers.wrap(getClient());
        final int numberOfFiles = Integer.valueOf(this.numberOfFiles);
        final long sizeOfFiles = Integer.valueOf(this.sizeOfFiles);

        try {
            try {
                final PutBucketRequest request = new PutBucketRequest(bucketName);
                getClient().putBucket(request);
            } catch(final FailedRequestException e) {
                this.doNotDelete = true;
                if (e.getStatusCode() == 409) {
                    throw new CommandException("Bucket " + bucketName + " already exists. To avoid any conflicts please use a non-existent bucket.");
                }
                throw new CommandException("Encountered a DS3 Error", e);
            }

            final List<Ds3Object> objList = getDs3Objects(numberOfFiles, sizeOfFiles);

            /**** PUT ****/
            transfer(helpers, numberOfFiles, sizeOfFiles, objList, true);

            /**** GET ****/
            transfer(helpers, numberOfFiles, sizeOfFiles, objList, false);

        } finally {
            if (!doNotDelete) deleteAllContents(getClient(), this.bucketName);
        }
        return new PerformanceResult("Done!");
    }

    private List<Ds3Object> getDs3Objects(final int numberOfFiles, final long sizeOfFiles) {
        final List<Ds3Object> objList = Lists.newArrayList();
        for (int i = 0; i < numberOfFiles; i++) {
            final long testFileSize = sizeOfFiles * 1024L * 1024L;
            final Ds3Object obj = new Ds3Object("file_" + i, testFileSize);
            objList.add(obj);
        }
        return objList;
    }

    private void transfer(final Ds3ClientHelpers helpers, final int numberOfFiles, final long sizeOfFiles, final List<Ds3Object> objList, final boolean isPutCommand) throws SignatureException, IOException, XmlProcessingException {
        final Ds3ClientHelpers.Job job;
        if (isPutCommand) {
            job = helpers.startWriteJob(this.bucketName, objList);
        } else {
            job = helpers.startReadJob(this.bucketName, objList);
        }
        job.withMaxParallelRequests(this.numberOfThreads);

        final PerformanceListener getPerformanceListener = new PerformanceListener(
                System.currentTimeMillis(),
                numberOfFiles,
                numberOfFiles * sizeOfFiles,
                isPutCommand);
        job.attachObjectCompletedListener(getPerformanceListener);
        job.attachDataTransferredListener(getPerformanceListener);
        job.transfer(new MemoryObjectChannelBuilder(bufferSize, sizeOfFiles));
        System.out.println();
    }

    private void deleteAllContents(final Ds3Client client, final String bucketName) throws IOException, SignatureException {
        final Ds3ClientHelpers helpers = Ds3ClientHelpers.wrap(client);
        final Iterable<Contents> objects = helpers.listObjects(bucketName);
        for (final Contents contents : objects) {
            client.deleteObject(new DeleteObjectRequest(bucketName, contents.getKey()));
        }

        client.deleteBucket(new DeleteBucketRequest(bucketName));
    }

    private class PerformanceListener implements DataTransferredListener, ObjectCompletedListener {
        private final long startTime;
        private final int totalNumberOfFiles;
        private final long numberOfMB;
        private final boolean isPutCommand;
        private long totalByteTransferred = 0;
        private int numberOfFiles = 0;
        private double highestMbps = 0.0;
        private double time;
        private long content;
        private double mbps;

        public PerformanceListener(final long startTime, final int totalNumberOfFiles, final long numberOfMB, final boolean isPutCommand) {
            this.startTime = startTime;
            this.totalNumberOfFiles = totalNumberOfFiles;
            this.numberOfMB = numberOfMB;
            this.isPutCommand = isPutCommand;
        }

        @Override
        public void dataTransferred(final long size) {

            final long currentTime = System.currentTimeMillis();
            synchronized (this) {
                totalByteTransferred += size;
                time = (currentTime - this.startTime == 0) ? 1.0 : (currentTime - this.startTime) / 1000D;
                content = totalByteTransferred / 1024L / 1024L;
                mbps = content / time;
                if (mbps > highestMbps) highestMbps = mbps;
            }
            printStatistics();
        }

        @Override
        public void objectCompleted(final String s) {
            synchronized (this) {
                numberOfFiles += 1;
            }
            printStatistics();
        }

        private void printStatistics() {
            final String messagePrefix;
            if (isPutCommand) {
                messagePrefix = "Putting";
            }
            else {
                messagePrefix = "Getting";
            }


            System.out.print(String.format("\r%s Statistics: (%d/%d MB), files (%d/%d completed), Time (%.03f sec), MBps (%.03f), Highest MBps (%.03f)",
                    messagePrefix, content, numberOfMB, numberOfFiles, totalNumberOfFiles, time, mbps, highestMbps));
        }
    }

}


