const {capitalizeFirstLetters, isMemberStaff} = require("../../../../utils/utilities");
const {MessageEmbed} = require('discord.js');
const {serverIP, serverPort} = require("../../../../configs/config.json");
const {CREATE} = require('../../../../configs/embed_thumbnails.json');
const axios = require("axios");

module.exports = {
    async execute(interaction) {

        if (!isMemberStaff(interaction)) {
            await interaction.editReply({content: "You don't have permission to use this command.", ephemeral: false});
            return;
        }

        const user = interaction.options.getUser("target-player").id;
        //name and title won't get capitalized here so people have more freedom when naming their chars
        const name = capitalizeFirstLetters(interaction.options.getString('name'));
        const title = capitalizeFirstLetters(interaction.options.getString('title'));
        const gear = capitalizeFirstLetters(interaction.options.getString('gear').toLowerCase());
        const pvp = interaction.options.getBoolean('pvp');

        //data sent to server
        const data = {
            discordId: user,
            rpCharName: name,
            title: title,
            gear: gear,
            pvp: pvp
        }

        axios.post('http://'+serverIP+':'+serverPort+'/api/player/create/rpchar', data)
            .then(async function() {
                //if request successful
                const replyEmbed = new MessageEmbed()
                    .setTitle(`Create RpChar`)
                    .setColor('GREEN')
                    .setDescription(`The Roleplay Character ${name} - ${title} has been created!`)
                    .setThumbnail(CREATE)
                    .setTimestamp()
                await interaction.editReply({embeds: [replyEmbed]});
            })
            .catch(async function(error) {
                //error occurred
                const replyEmbed = new MessageEmbed()
                    .setTitle(`Error while creating Roleplay Character`)
                    .setColor('RED')
                    .setDescription(error.response.data.message)
                    .setTimestamp()
                await interaction.editReply({embeds: [replyEmbed]});
            })

    },
};