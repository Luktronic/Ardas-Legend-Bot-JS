package com.ardaslegends.presentation.discord.utils;

public enum Thumbnails {

    MOVE_CHARACTER("https://cdn.discordapp.com/attachments/993179687956258867/993202523475292290/move_character.jpg"),
    INJURE_CHARACTER("https://cdn.discordapp.com/attachments/1022950091214041108/1022951678598721597/unknown.png"),
    HEAL("https://cdn.discordapp.com/attachments/993179687956258867/993202461743517766/heal.jpg"),
    DECLARE_BATTLE("https://cdn.discordapp.com/attachments/993179687956258867/993202288212582450/battle.jpg"),
    END_WAR("https://cdn.discordapp.com/attachments/993179687956258867/993202619281588355/peace.jpg"),
    BIND_TRADER("https://cdn.discordapp.com/attachments/993179687956258867/993202395913916416/create_trader.jpeg");

    private final String url;

    Thumbnails(String url) {
        this.url = url;
    }

    public String getUrl() {
        return url;
    }
}
