import { DynamoDBClient } from '@aws-sdk/client-dynamodb';
import { DynamoDBDocumentClient, GetCommand } from '@aws-sdk/lib-dynamodb';

const db = DynamoDBDocumentClient.from(new DynamoDBClient({}));

export const handler = async (event) => {
  try {
    const applicationId = event.pathParameters?.applicationId;
    if (!applicationId) throw new Error('applicationId is required');

    const result = await db.send(new GetCommand({
      TableName: process.env.APPLICATIONS_TABLE,
      Key: { applicationId }
    }));

    if (!result.Item) {
      return { statusCode: 404, headers: { 'content-type': 'application/json' }, body: JSON.stringify({ status: 'NOT_FOUND' }) };
    }

    return {
      statusCode: 200,
      headers: { 'content-type': 'application/json', 'access-control-allow-origin': '*' },
      body: JSON.stringify(result.Item)
    };
  } catch (error) {
    return { statusCode: 400, headers: { 'content-type': 'application/json' }, body: JSON.stringify({ error: error.message }) };
  }
};
