import boto3
from botocore.exceptions import ClientError

def delete_non_live_lambda_versions(function_name, region):
    client = boto3.client('lambda', region_name=region)

    try:
        aliases_response = client.list_aliases(FunctionName=function_name)
        live_versions = [
            alias['FunctionVersion']
            for alias in aliases_response.get('Aliases', [])
            if 'FunctionVersion' in alias
        ]

        print(f"Versions live: {live_versions}")

        versions_response = client.list_versions_by_function(
            FunctionName=function_name
        )

        deleted_count = 0
        for version in versions_response.get('Versions', []):
            version_number = version['Version']

            if version_number == '$LATEST':
                continue

            if version_number in live_versions:
                continue

            try:
                client.delete_function(
                    FunctionName=function_name,
                    Qualifier=version_number
                )
                print(f"Version {version_number} deleted")
                deleted_count += 1

            except ClientError as e:
                if e.response['Error']['Code'] == 'ResourceNotFoundException':
                    print(f"  Version {version_number} already deleted")
                else:
                    raise e

        print(f"Total number of deleted versions = {deleted_count}")

    except ClientError as e:
        print(f"Found error on {function_name}: {e.response['Error']['Message']}")


def cleanup_all_lambdas(region):
    client = boto3.client('lambda', region_name=region)

    paginator = client.get_paginator('list_functions')

    for page in paginator.paginate():
        for function in page['Functions']:
            function_name = function['FunctionName']
            print(f"Processing : {function_name}")
            delete_non_live_lambda_versions(function_name, region)


if __name__ == "__main__":
    cleanup_all_lambdas("eu-west-3")
