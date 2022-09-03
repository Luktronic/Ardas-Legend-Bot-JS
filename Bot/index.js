// Require the necessary discord.js classes
const fs = require('fs');
const {Client, Collection, Intents} = require('discord.js');
const {token} = require('./configs/bot_token.json');
const log4js = require("log4js");

// Create a new client instance
const client = new Client({
    intents: [Intents.FLAGS.GUILDS, Intents.FLAGS.GUILD_MESSAGES, Intents.FLAGS.GUILD_MESSAGE_REACTIONS],
    partials: ['MESSAGE', 'CHANNEL', 'REACTION']
});
client.commands = new Collection();
const commandFiles = fs.readdirSync('./commands').filter(file => file.endsWith('.js'));
const adminCommandFiles = fs.readdirSync('./commands/admin').filter(file => file.endsWith('.js'));
const eventFiles = fs.readdirSync('./events').filter(file => file.endsWith('.js'));

for (const file of commandFiles) {
    const command = require(`./commands/${file}`);
    // Set a new item in the Collection
    // With the key as the command name and the value as the exported module
    client.commands.set(command.data.name, command);
}
for (const file of adminCommandFiles) {
    const command = require(`./commands/admin/${file}`);
    client.commands.set(command.data.name, command);
}
for (const file of eventFiles) {
    const event = require(`./events/${file}`);
    if (event.once) {
        client.once(event.name, (...args) => event.execute(...args));
    } else {
        client.on(event.name, (...args) => event.execute(...args));
    }
}

log4js.configure({
    appenders: { file: { type: "file", filename: "ardaslegendsbot.log", compress: true}, console: { type: "console"} },
    categories: { default: { appenders: ["file", "console"], level: "debug"} },
});

// Login to Discord with your client's token
client.login(token).then();