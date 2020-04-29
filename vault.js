var _ = require('lodash')
const request = require('request');
// const vaultjwt = require('./create_vault_jwt')
var AWS = require('aws-sdk');

const lambda = new AWS.Lambda({ region: 'us-east-1' });

if (!process.env.AskFunction) {
    throw new Error('Could not find environment variable \'AskFunction\'')
}

const pre_allocate_instance = async function (session, datapoint) {
    datapoint.Parameters['_empower_base_url'] = await get_los_api(datapoint.Parameters.LOS)
    var res = await upload({
        metadata: {
            _remember_id: session.TransactionId,
            _datapoint: datapoint.DataPointName,
        },
        data: datapoint.Parameters
    });
    console.log("==============pre_allocate_instance res===========", res);
    
    return res;
}

const get_instance = function (session, datapoint) {
    console.log('datapoint params', datapoint.Parameters)
    var params = {
        _remember_id: session.TransactionId,
        _datapoint: datapoint.DataPointName,
    }
    if (datapoint.Parameters.FileId) {
        params['FileId'] = datapoint.Parameters.FileId
    }
    var res = get(params)

    return res.then((resp) => {
        console.log("resp......", resp)

        // res = res[datapoint.DataPointName]
        // _.each(res, function (r) {
        //     delete r.TransactionId
        //     delete r.message
        // })
        return resp
    })
}

const upload = async function (params, action) {
    try {
        var vaultFunction = process.env.ApiGatewayLambda;
        if (!vaultFunction) {
            throw new Error('Could not find environment variable \'VaultFunction\'')
        }
        console.log('upload params: ', params);
        const vaultparams = {
            "documentId": "",
            "fileName": params.data.FileName,
            "fileSize": params.data.size,
            "correlationId": params.data._loan_number,
            "transactionId": params.data._loan_number,
            "docType": params.data.documentType,
            "mimeType": params.data.contentType,
            "additionalData": {
                "FileName": params.data.FileName,
                "name": params.data.name,
                "createdOn": params.data.createdOn,
                "createdBy": params.data.createdBy,
                "category": params.data.category,
                "extension": params.data.extension,
                "createdOn": params.data.createdOn,
            }
        };

        var options = {
            httpMethod: "POST",
            path: "/uploadPut",
            resource: "/uploadPut",
            body: JSON.stringify(vaultparams),
            headers: {
                accept: "application/json",
                'content-type': "application/json",
            },
            "requestContext": {
                "identity": {
                    "cognitoIdentityPoolId": null,
                    "accountId": null,
                    "cognitoIdentityId": null,
                    "caller": null,
                    "apiKey": null,
                    "sourceIp": "8.39.161.192",
                    "cognitoAuthenticationType": null,
                    "cognitoAuthenticationProvider": null,
                    "userArn": null,
                    "userAgent": null,
                    "user": null
                },
                "resourcePath": "/uploadPut",
                "httpMethod": "POST",
            }
        }

        const lambdaParams = {
            FunctionName: process.env.ApiGatewayLambda,
            Payload: JSON.stringify(options)
        };
        console.log("====params ====lambdaInvoke===", lambdaParams);
        const lambdaInvoke = await lambda.invoke(lambdaParams).promise();
        console.log("====lambdaInvoke =======", lambdaInvoke);
        const payloadres = JSON.parse(lambdaInvoke.Payload);
        console.log("====payloadres =======", payloadres);
        const finalResponse = JSON.parse(payloadres.body);
        console.log("====finalResponse =======", finalResponse);
        return finalResponse;
        // var options = {
        //     pathParameters: { method: 'uploadPut' },
        //     body: JSON.stringify(vaultparams),
        //     headers: {}

        // }
        // console.log('upload vault params: ', vaultparams)
        // console.log('options: ', options)
        // const uploadData = await makeRequest(options)

        // console.log("upload data:", uploadData)
        // var vaultUpload = {
        //     "Parameters": params.data,
        //     "FileId": uploadData.documentId,
        //     "TargetUrl": uploadData.url,
        //     "message": "",
        //     "transactionId": uploadData._loan_number
        // }
        // console.log("vault Upload:", vaultUpload)


    } catch (err) {
        console.log("====error in upload function =======", err);
        return err;
    }

}

const get = async function (params) {
    console.log('get params:', params)

    var vaultUrl = process.env.VaultUrl;
    if (!vaultUrl) {
        throw new Error('Could not find environment variable \'vaultUrl\'')
    }

    console.log('---------------download params: ', params)

    if (params.FileId) {

        console.log("Retrieving file with ID" + params.FileId + "....")
        // const options = {
        //     url: vaultUrl + '/download/' + params.FileId,
        //     method: 'GET',
        //     headers: {
        //         'accept': 'application/json',
        //         'content-type': 'application/json',
        //     },

        //     qs: params //
        // }
        var options = {
            pathParameters: { type: 'download', method: 'FileId' },
            body: JSON.stringify(params),
            headers: {}

        }
        const getDataFromDownload = await makeRequest(options)
        console.log('----------------get data', getDataFromDownload)

        var vaultGet =
            [{
                "Parameters": params,
                "FileId": _.get(JSON.parse(getDataFromDownload), "documentId"),
                "TargetUrl": _.get(JSON.parse(getDataFromDownload), "url")
            }]

        console.log("vault get(file retrieved):.......", vaultGet)
    }
    else {
        console.log("Retrieving file IDs for loan no." + params._remember_id + ".....")
        // const options = {
        //     url: vaultUrl + '/query',
        //     method: 'POST',
        //     headers: {
        //         'accept': 'application/json',
        //         'content-type': 'application/json',},
        //     json: {
        //         "correlationId": [params._remember_id] 
        //     }
        // }
        var options = {
            pathParameters: { method: 'query' },
            body: JSON.stringify(params),
            headers: {}

        }
        const getData = await makeRequest(options)
        console.log('get data: ............', getData)

        //map to array of objects:
        var vaultGet = _.map(_.get(getData, "documents"), (document) => ({
            Parameters:
            {
                "_loan_number": _.get(document, "correlationId"),
                "extension": _.get(document, "sourceData.extension"),
                "documentType": _.get(document, "docType"),
                "FileName": _.get(document, "fileName"),
                "createdOn": _.get(document, "sourceData.createdOn"),
                "_file": {
                    "_file_type": _.get(document, "sourceData.extension"),
                    "_file_name": _.get(document, "fileName"),
                },
                "size": _.get(document, "docSize"),
                "createdBy": "",
                "name": _.get(document, "fileName"),
                "loanNumber": _.get(document, "correlationId"),
                "category": _.get(document, "sourceData.category"),
                "contentType": _.get(document, "mimeType"),
                "_channel": "correspondent"
            }, FileId: _.get(document, "documentId")
        }));


        console.log("vault get:.......", vaultGet)

    }

    // return vaultGet
    return new Promise((resolve, reject) => {
        var params = {
            FunctionName: process.env.ApiGatewayLambda,
            Payload: vaultGet
        };
        lambda.invoke(params, function (err, data) {
            console.log("Vault Get Response: ", Payload.data.body);
            if (err) { console.error(err); resolve(err); }
            resolve(Payload.data.body)

        });
    });
}

const makeRequest = function (options) {
    return new Promise((resolve, reject) => {
        request(options, (error, response, body) => {
            console.log(body);
            if (error) {
                console.log(body);
                console.error(error);
                resolve(error);
            } else if (response) {
                console.log('status code: ', response.statusCode)
                if (response.statusCode >= 400) {
                    resolve(response);
                    return;
                }
                console.log('headers: ', response.headers)
                if (options.qs) {
                    body = JSON.parse(body)
                }
                resolve(body)
            }
        });
    })
}

const call_lambda = function (payload) {
    return new Promise((resolve, reject) => {
        var params = {
            FunctionName: process.env.AskFunction,
            Payload: JSON.stringify(payload)
        };
        lambda.invoke(params, function (err, data) {
            if (err) { console.error(err); resolve(err); }
            resolve(JSON.parse(JSON.parse(data.Payload).body))
        })
    })
}

var get_los_api = function (los) {
    const api_list = require('./api_list.json').los_list
    return _.filter(api_list, function (m) {
        return m.Name == los
    })
}

module.exports = {
    makeRequest,
    call_lambda,
    get_instance,
    pre_allocate_instance,
    get_los_api
}
