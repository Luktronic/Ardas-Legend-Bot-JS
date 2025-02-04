package com.ardaslegends.util;

import com.ardaslegends.domain.*;
import lombok.val;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class DeprecatedTestDataFactory {

    public static Player playerLuktronic() {
        val luktronic = Player.builder()
                .id(1L)
                .ign("Luktronic")
                .discordID("261173268365443074")
                .uuid("cefabe13fda44d378c5d7292724f1514")
                .isStaff(true)
                .build();
        luktronic.setFaction(factionGondor(luktronic));
        luktronic.addActiveRpChar(rpcharBelegorn(luktronic));
        return luktronic;
    }

    public static Player playerLuktronic(RPChar rpChar) {
        val luktronic = Player.builder()
                .id(1L)
                .ign("Luktronic")
                .discordID("261173268365443074")
                .uuid("cefabe13fda44d378c5d7292724f1514")
                .isStaff(true)
                .build();
        luktronic.setFaction(factionGondor(luktronic));
        luktronic.addActiveRpChar(rpChar);
        return luktronic;
    }

    public static Player playerLuktronic(Faction faction) {
        return Player.builder()
                .id(1L)
                .ign("Luktronic")
                .discordID("261173268365443074")
                .uuid("cefabe13fda44d378c5d7292724f1514")
                .isStaff(true)
                .faction(faction)
                .build();
    }

    public static Player playerMirak(Faction faction) {
        return Player.builder()
                .id(2L)
                .ign("mirak441")
                .discordID("244463773052567553")
                .uuid("4cd6b222b3894fd59d85ac90aa2c2c46")
                .isStaff(true)
                .faction(faction)
                .build();
    }

    public static Player playerHabKeinTeammate(Faction faction) {
        return Player.builder()
                .id(3L)
                .ign("HabKeinTeammate")
                .discordID("323522559096258582")
                .uuid("84b6a14958ec4b2bb9b479328526651d")
                .isStaff(false)
                .faction(faction)
                .build();

    }

    public static Player playerVernonRoche(Faction faction) {
        val player = new Player("VernonRoche", "866830b12e944a97918439282412c487", "253505646190657537", faction, RPChar.builder().build());
        player.setRpChars(Set.of());
        return player;
    }

    public static RPChar rpcharBelegorn(Player player) {
        val belegorn = RPChar.builder()
                .name("Belegorn")
                .gear("All")
                .title("King of Gondor")
                .pvp(true)
                .healEnds(null)
                .isHealing(false)
                .boundTo(null)
                .active(false)
                .injured(false)
                .linkToLore(null)
                .startedHeal(null)
                .currentRegion(region263())
                .owner(player)
                .build();

        return belegorn;
    }

    public static Faction factionGondor() {
        val region263 = region263();
        val gondor = Faction.builder().name("Gondor").colorcode("#e9e9e9")
                .factionBuffDescr("2x movement in Gondor owned regions.")
                .foodStockpile(0)
                .homeRegion(region263)
                .initialFaction(InitialFaction.GONDOR)
                .regions(new HashSet<>(Set.of(region263)))
                .aliases(new HashSet<>()) //TODO: add aliases
                .build();
        val luktronic = playerLuktronic(gondor);
        gondor.setLeader(luktronic);
        gondor.setPlayers(new ArrayList<>(List.of(luktronic)));
        return gondor;
    }

    public static Faction factionGondor(Player leader) {
        val region263 = region263();
        val gondor = Faction.builder().name("Gondor").colorcode("#e9e9e9")
                .factionBuffDescr("2x movement in Gondor owned regions.")
                .foodStockpile(0)
                .homeRegion(region263)
                .initialFaction(InitialFaction.GONDOR)
                .regions(new HashSet<>(Set.of(region263)))
                .aliases(new HashSet<>()) //TODO: add aliases
                .build();
        gondor.setLeader(leader);
        val players = new ArrayList<Player>();
        if(leader != null)
            players.add(leader);
        gondor.setPlayers(players);
        return gondor;
    }

    public static Faction factionMordor(Player leader) {
        val region267 = region267();
        val mordor = Faction.builder().name("Mordor").colorcode("#ff0000")
                .factionBuffDescr("The Faction Leader's army can build \"Grond\" which is a ram that instantly breaks gates instead of taking 3 hits. Does not need players nearby to move it. Enemy armies that enter geographical Mordor regions have their time to move through regions doubled.")
                .foodStockpile(0)
                .homeRegion(region267)
                .initialFaction(InitialFaction.GONDOR)
                .regions(new HashSet<>(Set.of(region267)))
                .aliases(new HashSet<>()) //TODO: add aliases
                .build();
        mordor.setLeader(leader);
        val players = new ArrayList<Player>();
        if(leader != null)
            players.add(leader);
        mordor.setPlayers(players);
        return mordor;
    }

    public static Region region263() {
        return Region.builder().id("263").regionType(RegionType.LAND).build();
    }
    public static Region region267() {
        return Region.builder().id("267").regionType(RegionType.LAND).build();
    }
}
