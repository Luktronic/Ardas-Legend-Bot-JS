package com.ardaslegends.util;

import com.ardaslegends.domain.*;
import lombok.extern.slf4j.Slf4j;
import lombok.val;
import org.junit.jupiter.api.Test;

import java.util.*;
import java.util.stream.Collectors;

/**
 * @author <a href="mailto:luktronic@gmx.at">Luktronic</a>
 */
@Slf4j
public class TestData {

	public static void loadModel() {
		val start = System.currentTimeMillis();

		Players.load();
		Regions.load();
		ProductionSites.load();
		Claimbuilds.load();
		Factions.loadFactionGondor();

		val end = System.currentTimeMillis();

		log.info("Test data model loaded in {}ms", end-start);
	}

	public static class Players {
		public static Player luktronic;
		public static Player mirak;

		public static void load() {
			loadPlayerLuktronic();
			loadPlayerMirak();
		}

		public static void loadPlayerLuktronic() {
			luktronic = new Player("Luktronic", UUID.randomUUID().toString(), "12345", null);
			luktronic.setIsStaff(true);
			luktronic.setRpChars(new HashSet<>(Set.of(RpChars.belegorn)));
			luktronic.getRpChars().stream().filter(Objects::nonNull)
					.forEach(rpChar -> rpChar.setOwner(luktronic));
		}


		public static void loadPlayerMirak() {
			mirak = new Player("mirak441", UUID.randomUUID().toString(), "6789", null);
			mirak.setIsStaff(true);
			mirak.setRpChars(new HashSet<>(Set.of(RpChars.firyawe)));
			mirak.getRpChars().stream().filter(Objects::nonNull)
					.forEach(rpChar -> rpChar.setOwner(mirak));
		}
	}

	public static class RpChars {
		public static RPChar belegorn;
		public static RPChar firyawe;

		public static void load() {
			loadBelegorn();
			loadFiryawe();
		}

		public static void loadBelegorn() {
			belegorn = new RPChar(Players.luktronic, "Belegorn", "King of Gondor", "Gondorian gear", true, "someLink");
		}

		public static void loadFiryawe() {
			firyawe = new RPChar(Players.mirak, "Firyawe", "Prince of Dol Amroth", "Dol Amroth", true, "someLink");
		}
	}

	public static class Regions {
		public static Region region100;
		public static Region region101;
		public static Region region102;
		public static Region region103;
		public static Region region104;
		public static Region region105;
		public static Region region106;
		public static Set<Region> all;

		public static void load() {
			val claimedByGondor = new HashSet<Faction>();
			claimedByGondor.add(Factions.gondor);

			val region100Cbs = new HashSet<ClaimBuild>();
			region100Cbs.add(Claimbuilds.gondorCastle);
			val region101Cbs = new HashSet<ClaimBuild>();
			region101Cbs.add(Claimbuilds.gondorTown);
			
			region100 = new Region("100", null, RegionType.LAND, claimedByGondor, region100Cbs, new HashSet<>());
			region101 = new Region("101", null, RegionType.LAND, claimedByGondor, region101Cbs, new HashSet<>());
			region102 = new Region("102", null, RegionType.LAND, new HashSet<>(), new HashSet<>(), new HashSet<>());
			region103 = new Region("103", null, RegionType.LAND, new HashSet<>(), new HashSet<>(), new HashSet<>());

			region104 = new Region("104", null, RegionType.MOUNTAIN, new HashSet<>(), new HashSet<>(), new HashSet<>());
			region105 = new Region("105", null, RegionType.MOUNTAIN, new HashSet<>(), new HashSet<>(), new HashSet<>());
			region106 = new Region("106", null, RegionType.MOUNTAIN, new HashSet<>(), new HashSet<>(), new HashSet<>());

			region100.setNeighboringRegions(Set.of(region101, region104));
			region101.setNeighboringRegions(Set.of(region100, region102, region104, region105));
			region102.setNeighboringRegions(Set.of(region101, region103, region105, region106));
			region103.setNeighboringRegions(Set.of(region102, region106));

			region104.setNeighboringRegions(Set.of(region100, region101, region105));
			region105.setNeighboringRegions(Set.of(region101, region102, region104, region106));
			region106.setNeighboringRegions(Set.of(region102, region103, region105));

			region100Cbs.stream().filter(Objects::nonNull)
					.forEach(cb -> cb.setRegion(region100));
			region101Cbs.stream().filter(Objects::nonNull)
					.forEach(cb -> cb.setRegion(region101));

			all = new HashSet<>();
			all.add(region100);
			all.add(region101);
			all.add(region102);
			all.add(region103);
			all.add(region104);
			all.add(region105);
			all.add(region106);
		}

		public static Set<Region> withIds(String... ids) {
			return all.stream()
					.filter(region -> Arrays.stream(ids).anyMatch(id -> region.getId().equals(id)))
					.collect(Collectors.toSet());
		}
	}

	public static class Claimbuilds {
		public static ClaimBuild gondorCastle;
		public static ClaimBuild gondorTown;
		public static Set<ClaimBuild> gondorBuilds;

		public static void load() {
			loadGondorClaimbuilds();
		}

		public static void loadGondorClaimbuilds() {
			loadGondorCastle();
			loadGondorTown();
			gondorBuilds = new HashSet<>();
			gondorBuilds.add(gondorCastle);
			gondorBuilds.add(gondorTown);
		}

		public static void loadGondorCastle() {
			val builtBy = new HashSet<>(Set.of(Players.luktronic));
			gondorCastle = new ClaimBuild("Gondor Castle", Regions.region100, ClaimBuildType.CASTLE, Factions.gondor, new Coordinate(1,1,1),
					new ArrayList<>(), "", "", "", builtBy);
		}


		public static void loadGondorTown() {
			val builtBy = new HashSet<>(Set.of(Players.luktronic, Players.mirak));
			gondorTown = new ClaimBuild("Gondor Town", Regions.region101, ClaimBuildType.TOWN, Factions.gondor, new Coordinate(1,1,1),
					new ArrayList<>(), "", "", "", builtBy);
			val prodClaimbuilds = new ArrayList<>(List.of(new ProductionClaimbuild(ProductionSites.wheatFarm, gondorTown, 5L)));
			gondorTown.setProductionSites(prodClaimbuilds);
		}
	}

	public static class ProductionSites {
		public static ProductionSite wheatFarm;

		public static void load() {
			loadWheatFarm();
		}

		public static void loadWheatFarm() {
			wheatFarm = new ProductionSite(1L, ProductionSiteType.FARM, Resources.wheat, 64);
			if(Resources.wheat == null)
				Resources.loadWheat();
		}

		public static class Resources {
			public static Resource wheat;

			public static void load() {
				loadWheat();
			}

			public static void loadWheat() {
				val wheatSites = new ArrayList<>(List.of(wheatFarm));
				wheat = new Resource(1L, "Wheat", "123", ResourceType.CROP, wheatSites);
				wheatSites.stream().filter(Objects::nonNull)
						.forEach(site -> site.setProducedResource(wheat));
			}
		}

	}

	public static class Factions {
		public static Faction gondor;

		public static void loadFactionGondor() {
			val players = new ArrayList<Player>();
			players.add(Players.luktronic);
			players.add(Players.mirak);

			val regions = new HashSet<>(Set.of(Regions.region100, Regions.region101));
			val claimbuilds = Claimbuilds.gondorBuilds.stream().toList();

			gondor = new Faction("Gondor", Players.luktronic, new ArrayList<>(), players, regions,
					claimbuilds, new ArrayList<>(), "w", Regions.region101, "w");

			regions.stream().filter(Objects::nonNull)
					.forEach(region -> region.setClaimedBy(Set.of(gondor)));
			claimbuilds.stream().filter(Objects::nonNull)
					.forEach(cb -> cb.setOwnedBy(gondor));
			players.stream().filter(Objects::nonNull)
					.forEach(player -> player.setFaction(gondor));
		}
	}


}
