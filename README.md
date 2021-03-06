# Cerberus Serverless Components

![Build](https://github.com/Nike-Inc/cerberus-serverless-components/workflows/Build/badge.svg)
[![][license img]][license]

This project contains the serverless components that can be used with Cerberus.

## Serverless Components

* [Cerberus Health Check](cerberus-health-check-lambda/README.md) - A small serverless API that authenticates and reads a secret from cerberus.
* [Cerberus Artemis KPI Lambda](cerberus-artemis-kpi-lambda/README.md) - Reference Example function for processing Cerberus metrics from the metrics topic
* [Cerberus Log Processor Lambda](cerberus-log-processor-lambda/README.md) - Serverless function for processing access log events, to enable things such as rate limiting.
* [Cerberus Lambda VPC](cerberus-lambda-vpc/README.md) - Cloudformation to create a VPC with EIBs and NATs so that Cerberus operators can run lambdas with predictable IP addresses.
* [Cerberus Metrics Topic](cerberus-metrics-topic/README.md) - Cloudformation to create a SNS topic that is used by various components to publish Cerberus metrics.
* [Cerberus Admin IAM Role](cerberus-admin-iam-role/README.md) - Creates the admin IAM role to be used by the Lambda serverless components to call the Cerberus Management Service admin endpoints.

## Profiles

**Read this first**

This project is configured to load properties from environment specific profiles. in the [profile/ directory](profile) 
there is [example.properties](profile/example.properties), an example profile. Before deploying any of these serverless components 
you will need to create profiles for your desired environment. 

1. Create a file named `global.properties` in the `profile` directory, and add all the global properties that are shared by all your environments. 
    - Almost all the props will go into global. 
1. Create your environment specific props files such as dev.properties and prod.properties.
    - Properties such as `cerberus.url` will go into the env specific props file.

We keep our profiles in a separate repo and create soft links to the profile dir in this project, and ignore them in the .gitignore file

## License

This project is released under the [Apache License, Version 2.0](http://www.apache.org/licenses/LICENSE-2.0)

[license]:LICENSE.txt
[license img]:https://img.shields.io/badge/License-Apache%202-blue.svg
