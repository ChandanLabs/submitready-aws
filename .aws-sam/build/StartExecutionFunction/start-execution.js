import { SFNClient, StartExecutionCommand } from '@aws-sdk/client-sfn';

const sfn = new SFNClient({});

export const handler = async (event) => {
  try {
    const applicationId = event.pathParameters?.applicationId;
    if (!applicationId) throw new Error('applicationId is required');

    const packet = typeof event.body === 'string' ? JSON.parse(event.body) : (event.body ?? {});
    const input = JSON.stringify({ applicationId, packet });

    const started = await sfn.send(new StartExecutionCommand({
      stateMachineArn: process.env.STATE_MACHINE_ARN,
      input
    }));

    return {
      statusCode: 202,
      headers: { 'content-type': 'application/json', 'access-control-allow-origin': '*' },
      body: JSON.stringify({
        applicationId,
        status: 'VALIDATION_STARTED',
        executionArn: started.executionArn
      })
    };
  } catch (error) {
    return {
      statusCode: 400,
      headers: { 'content-type': 'application/json', 'access-control-allow-origin': '*' },
      body: JSON.stringify({ error: error.message })
    };
  }
};
