import {TwitterApi, TwitterApiTokens} from "twitter-api-v2";

let client = new TwitterApi({
    appKey: process.env.X_CONSUMER_API_KEY,
    appSecret: process.env.X_CONSUMER_API_SECRET_KEY,
    accessToken: process.env.X_ACCESS_TOKEN,
    accessSecret: process.env.X_ACCESS_TOKEN_SECRET,
} as TwitterApiTokens).readWrite;

client.v2.tweet({
    text: "Hello, World!"
}).then(r => {
    console.log("Created Post:", r.data);
}).catch(e => {
    console.error(e)
});
