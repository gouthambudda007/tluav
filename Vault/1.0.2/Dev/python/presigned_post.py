# see:
# https://boto3.amazonaws.com/v1/documentation/api/latest/reference/services/s3.html#S3.Client.generate_presigned_post
# https://boto3.amazonaws.com/v1/documentation/api/latest/guide/s3-presigned-urls.html#generating-a-presigned-url-to-upload-a-file
# https://stackoverflow.com/questions/34348639/amazon-aws-s3-browser-based-upload-using-post
#
import logging
import os

import boto3
from botocore.client import Config

import json
import datetime

from botocore.exceptions import ClientError

def lambda_handler(event, context):
    print("Received event: " + json.dumps(event, indent=2))

    bucket_name = event["bucketName"]
    object_key = event["objectKey"]
    expiration = event["expiration"]

    max_file_size = int(os.environ['MAX_FILE_SIZE'])
   
    fields = {
        'success_action_status': '201'
    }
    
    conditions = [
        {'success_action_status': '201'},
        ['content-length-range', 0, max_file_size]
    ]
    
    '''
    fields = { 
        'acl': 'private',
        'date': date_short,
        'region': region,
        'x-amz-algorithm': 'AWS4-HMAC-SHA256',
        'x-amz-date': date_long.
        'success_action_status', integer
    }
    
    conditions = [
            #{'acl': 'private'},
            #{'x-amz-algorithm': 'AWS4-HMAC-SHA256'},
            #{'x-amz-credential': '/'.join(['AKI--snip--', date_short, region, 's3', 'aws4_request'])},
            #{'x-amz-date': date_long},
            {'success_action_status', 201},
            ["content-length-range", 10, 100]
        ]
    
    ''' 
    
    result = create_presigned_post(bucket_name, object_key, fields, conditions, expiration)
    print("Result: " + json.dumps(result, indent=2))

    return result

def create_presigned_post(bucket_name, object_key,
                          fields=None, conditions=None, expiration=3600):
    """Generate a presigned URL S3 POST request to upload a file

    :param bucket_name: string
    :param object_key: string
    :param fields: Dictionary of prefilled form fields
    :param conditions: List of conditions to include in the policy
    :param expiration: Time in seconds for the presigned URL to remain valid
    :return: Dictionary with the following keys:
        url: URL to post to
        fields: Dictionary of form fields and values to submit with the POST
    :return: None if error.
    """

    # configure for signature v4:
    s3_client = boto3.client('s3', config=Config(signature_version='s3v4'))
    
    # Generate a presigned S3 POST URL
    try:
        response = s3_client.generate_presigned_post(bucket_name,
                                                     object_key,
                                                     Fields=fields,
                                                     Conditions=conditions,
                                                     ExpiresIn=expiration)
    except ClientError as e:
        logging.error(e)
        return None

    # The response contains the presigned URL and required fields
    return response
