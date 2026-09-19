import { DynamoDBClient } from '@aws-sdk/client-dynamodb';
import { DynamoDBDocumentClient, PutCommand } from '@aws-sdk/lib-dynamodb';

const db = DynamoDBDocumentClient.from(new DynamoDBClient({}));

export const handler = async (event) => {
  const { applicationId, packet, report } = event;
  if (!applicationId || !report) throw new Error('applicationId and report are required');

  await db.send(new PutCommand({
    TableName: process.env.APPLICATIONS_TABLE,
    Item: {
      applicationId,
      status: report.ready ? 'READY_TO_SUBMIT' : 'FIXES_REQUIRED',
      passedCount: report.passedCount,
      totalChecks: report.totalChecks,
      report,
      updatedAt: new Date().toISOString(),
      // Store the packet separately only when the deployment enables it.
      packet
    }
  }));

  return { applicationId, status: report.ready ? 'READY_TO_SUBMIT' : 'FIXES_REQUIRED', report };
};
