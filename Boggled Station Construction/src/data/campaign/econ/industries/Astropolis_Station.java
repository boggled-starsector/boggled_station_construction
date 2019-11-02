package data.campaign.econ.industries;

import java.awt.Color;
import java.util.Iterator;
import java.util.Random;
import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.campaign.*;
import com.fs.starfarer.api.impl.campaign.ids.*;
import com.fs.starfarer.api.impl.campaign.procgen.StarAge;
import com.fs.starfarer.api.impl.campaign.procgen.StarSystemGenerator;
import com.fs.starfarer.api.impl.campaign.submarkets.StoragePlugin;
import com.fs.starfarer.api.impl.campaign.terrain.BaseTiledTerrain;
import com.fs.starfarer.api.impl.campaign.terrain.MagneticFieldTerrainPlugin.MagneticFieldParams;
import com.fs.starfarer.api.util.Misc;
import com.fs.starfarer.api.campaign.econ.MarketAPI;
import com.fs.starfarer.api.impl.campaign.econ.impl.BaseIndustry;
import com.fs.starfarer.api.impl.campaign.ids.Conditions;
import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.campaign.LocationAPI;
import com.fs.starfarer.api.campaign.SectorAPI;
import com.fs.starfarer.api.campaign.SectorEntityToken;
import com.fs.starfarer.api.campaign.StarSystemAPI;
import com.fs.starfarer.api.impl.campaign.CoreCampaignPluginImpl;
import com.fs.starfarer.api.impl.campaign.CoreScript;
import com.fs.starfarer.api.impl.campaign.events.CoreEventProbabilityManager;
import com.fs.starfarer.api.impl.campaign.fleets.DisposableLuddicPathFleetManager;
import com.fs.starfarer.api.impl.campaign.fleets.DisposablePirateFleetManager;
import com.fs.starfarer.api.impl.campaign.fleets.EconomyFleetRouteManager;
import com.fs.starfarer.api.impl.campaign.fleets.MercFleetManagerV2;
import com.fs.starfarer.api.impl.campaign.ids.Factions;
import com.fs.starfarer.api.impl.campaign.ids.Terrain;
import com.fs.starfarer.api.impl.campaign.procgen.NebulaEditor;
import com.fs.starfarer.api.impl.campaign.terrain.HyperspaceTerrainPlugin;
import com.fs.starfarer.api.util.Misc;
import com.fs.starfarer.campaign.*;
import com.fs.starfarer.combat.entities.terrain.Planet;
import data.campaign.econ.BoggledStationConstructionIDs;

public class Astropolis_Station extends BaseIndustry {

    @Override
    public void apply() {
        super.apply(true);
    }

    @Override
    public void unapply() {
        super.unapply();
    }

    @Override
    public void finishBuildingOrUpgrading() {
        super.finishBuildingOrUpgrading();
    }

    private int numAstroInOrbit()
    {
        int numAstropoli = 0;
        Iterator allEntitiesInSystem = this.market.getStarSystem().getAllEntities().iterator();

        while(allEntitiesInSystem.hasNext())
        {
            SectorEntityToken entity = (SectorEntityToken)allEntitiesInSystem.next();
            if (entity.getOrbitFocus() != null && entity.getOrbitFocus().equals(this.market.getPrimaryEntity()) && entity.getId().contains("boggled_astropolis"))
            {
                numAstropoli++;
            }
        }

        return numAstropoli;
    }

    private float randomOrbitalAngleFloat(float min, float max)
    {
        Random rand = new Random();
        return rand.nextFloat() * (max - min) + min;
    }

    @Override
    protected void buildingFinished()
    {
        super.buildingFinished();

        //Check if the market should be placed on an existing station
        SectorEntityToken targetEntityForMarket = null;
        String marketNameSymbol = null;
        Iterator allEntitiesInSystem1 = this.market.getStarSystem().getAllEntities().iterator();
        while(allEntitiesInSystem1.hasNext())
        {
            SectorEntityToken entity = (SectorEntityToken)allEntitiesInSystem1.next();
            if (entity.getOrbitFocus() != null && entity.getOrbitFocus().equals(this.market.getPrimaryEntity()) && entity.getId().contains("boggled_astropolis") && entity.getFaction().getId().equals("neutral"))
            {
                if(entity.getId().contains("boggled_astropolisAlpha"))
                {
                    targetEntityForMarket = entity;
                    marketNameSymbol = "alpha";
                }
                else if(entity.getId().contains("boggled_astropolisBeta"))
                {
                    targetEntityForMarket = entity;
                    marketNameSymbol = "beta";
                }
                else if(entity.getId().contains("boggled_astropolisGamma"))
                {
                    targetEntityForMarket = entity;
                    marketNameSymbol = "gamma";
                }
            }
        }

        if(targetEntityForMarket != null)
        {
            targetEntityForMarket.setFaction(this.market.getFactionId());
            CargoAPI cargo = targetEntityForMarket.getMarket().getSubmarket("storage").getCargo();

            //Create the astropolis market
            MarketAPI market = Global.getFactory().createMarket("astropolis" + marketNameSymbol + "Market", targetEntityForMarket.getName(), 3);
            market.setSize(3);

            market.setSurveyLevel(MarketAPI.SurveyLevel.FULL);
            market.setPrimaryEntity(targetEntityForMarket);

            market.setFactionId(this.market.getFactionId());
            market.setPlayerOwned(true);

            market.addCondition(Conditions.POPULATION_3);

            market.addIndustry(Industries.POPULATION);
            market.addIndustry(Industries.SPACEPORT);

            targetEntityForMarket.setMarket(market);

            Global.getSector().getEconomy().addMarket(market, true);

            //If the player doesn't view the colony management screen within a few days of market creation, then there can be a bug related to population growth
            Global.getSector().getCampaignUI().showInteractionDialog(targetEntityForMarket);

            market.addSubmarket("storage");
            StoragePlugin storage = (StoragePlugin)market.getSubmarket("storage").getPlugin();
            storage.setPlayerPaidToUnlock(true);
            market.addSubmarket("local_resources");

            if(!cargo.isEmpty())
            {
                market.getSubmarket("storage").getCargo().addAll(cargo);
            }
        }

        int numAstro = numAstroInOrbit();
        StarSystemAPI system = this.market.getStarSystem();
        float orbitRadius = this.market.getPrimaryEntity().getRadius() + 375.0F;

        if(numAstro == 0 && targetEntityForMarket == null)
        {
            SectorEntityToken newAstropolis = system.addCustomEntity("boggled_astropolisAlpha", this.market.getPrimaryEntity().getName() + " Astropolis Alpha", "boggled_astropolis_station_alpha", this.market.getFactionId());
            newAstropolis.setCircularOrbitPointingDown(this.market.getPrimaryEntity(), randomOrbitalAngleFloat(0f, 360f), orbitRadius, orbitRadius / 10.0F);
            newAstropolis.setInteractionImage("illustrations", "orbital_construction");

            //Create the astropolis market
            MarketAPI market = Global.getFactory().createMarket("astropolisAlphaMarket", newAstropolis.getName(), 3);
            market.setSize(3);

            market.setSurveyLevel(MarketAPI.SurveyLevel.FULL);
            market.setPrimaryEntity(newAstropolis);

            market.setFactionId(this.market.getFactionId());
            market.setPlayerOwned(true);

            market.addCondition(Conditions.POPULATION_3);

            market.addIndustry(Industries.POPULATION);
            market.addIndustry(Industries.SPACEPORT);

            newAstropolis.setMarket(market);

            Global.getSector().getEconomy().addMarket(market, true);

            //If the player doesn't view the colony management screen within a few days of market creation, then there can be a bug related to population growth
            Global.getSector().getCampaignUI().showInteractionDialog(newAstropolis);

            market.addSubmarket("storage");
            StoragePlugin storage = (StoragePlugin)market.getSubmarket("storage").getPlugin();
            storage.setPlayerPaidToUnlock(true);
            market.addSubmarket("local_resources");
        }
        else if(numAstro == 1 && targetEntityForMarket == null)
        {
            Iterator allEntitiesInSystem = this.market.getStarSystem().getAllEntities().iterator();
            SectorEntityToken alphaAstroToken = null;

            while(allEntitiesInSystem.hasNext())
            {
                SectorEntityToken entity = (SectorEntityToken)allEntitiesInSystem.next();
                if (entity.getOrbitFocus() != null && entity.getOrbitFocus().equals(this.market.getPrimaryEntity()) && entity.getId().contains("astropolisAlpha"))
                {
                    alphaAstroToken = entity;
                    break;
                }
            }

            SectorEntityToken newAstropolis = system.addCustomEntity("boggled_astropolisBeta", this.market.getPrimaryEntity().getName() + " Astropolis Beta", "boggled_astropolis_station_beta", this.market.getFactionId());
            newAstropolis.setCircularOrbitPointingDown(this.market.getPrimaryEntity(), alphaAstroToken.getCircularOrbitAngle() + 120f, orbitRadius, orbitRadius / 10.0F);
            newAstropolis.setInteractionImage("illustrations", "orbital_construction");

            //Create the astropolis market
            MarketAPI market = Global.getFactory().createMarket("astropolisBetaMarket", newAstropolis.getName(), 3);
            market.setSize(3);
            market.setFactionId(this.market.getFactionId());

            market.setSurveyLevel(MarketAPI.SurveyLevel.FULL);
            market.setPrimaryEntity(newAstropolis);

            market.setFactionId(this.market.getFactionId());
            market.addCondition(Conditions.POPULATION_3);

            market.addIndustry(Industries.POPULATION);
            market.addIndustry(Industries.SPACEPORT);

            newAstropolis.setMarket(market);
            market.setPlayerOwned(true);

            Global.getSector().getEconomy().addMarket(market, true);

            //If the player doesn't view the colony management screen within a few days of market creation, then there can be a bug related to population growth
            Global.getSector().getCampaignUI().showInteractionDialog(newAstropolis);

            market.addSubmarket("storage");
            StoragePlugin storage = (StoragePlugin)market.getSubmarket("storage").getPlugin();
            storage.setPlayerPaidToUnlock(true);
            market.addSubmarket("local_resources");
        }
        else if(numAstro == 2 && targetEntityForMarket == null)
        {
            Iterator allEntitiesInSystem = this.market.getStarSystem().getAllEntities().iterator();
            SectorEntityToken alphaAstroToken = null;

            while(allEntitiesInSystem.hasNext())
            {
                SectorEntityToken entity = (SectorEntityToken)allEntitiesInSystem.next();
                if (entity.getOrbitFocus() != null && entity.getOrbitFocus().equals(this.market.getPrimaryEntity()) && entity.getId().contains("astropolisAlpha"))
                {
                    alphaAstroToken = entity;
                    break;
                }
            }

            SectorEntityToken newAstropolis = system.addCustomEntity("boggled_astropolisGamma", this.market.getPrimaryEntity().getName() + " Astropolis Gamma", "boggled_astropolis_station_gamma", this.market.getFactionId());
            newAstropolis.setCircularOrbitPointingDown(this.market.getPrimaryEntity(), alphaAstroToken.getCircularOrbitAngle() - 120f, orbitRadius, orbitRadius / 10.0F);
            newAstropolis.setInteractionImage("illustrations", "orbital_construction");

            //Create the astropolis market
            MarketAPI market = Global.getFactory().createMarket("astropolisGammaMarket", newAstropolis.getName(), 3);
            market.setSize(3);
            market.setFactionId(this.market.getFactionId());

            market.setSurveyLevel(MarketAPI.SurveyLevel.FULL);
            market.setPrimaryEntity(newAstropolis);

            market.setFactionId(this.market.getFactionId());
            market.addCondition(Conditions.POPULATION_3);

            market.addIndustry(Industries.POPULATION);
            market.addIndustry(Industries.SPACEPORT);

            newAstropolis.setMarket(market);
            market.setPlayerOwned(true);

            Global.getSector().getEconomy().addMarket(market, true);

            //If the player doesn't view the colony management screen within a few days of market creation, then there can be a bug related to population growth
            Global.getSector().getCampaignUI().showInteractionDialog(newAstropolis);

            market.addSubmarket("storage");
            StoragePlugin storage = (StoragePlugin)market.getSubmarket("storage").getPlugin();
            storage.setPlayerPaidToUnlock(true);
            market.addSubmarket("local_resources");
        }

        this.market.removeIndustry("ASTROPOLIS_STATION",null,false);
    }

    @Override
    public void startBuilding() {
        super.startBuilding();
    }

    private boolean astropolisOrbitOK()
    {
        SectorEntityToken hostToken = this.market.getPrimaryEntity();

        //check if the host market radius is too small
        if(hostToken.getRadius() < 125f)
        {
            return false;
        }

        //check if the host market is too close to its orbital focus
        if(hostToken.getCircularOrbitRadius() < (hostToken.getOrbitFocus().getRadius() + 900f))
        {
            return false;
        }

        //check if the host market has a moon that is too close to it
        Iterator allPlanetsInSystem = this.market.getStarSystem().getPlanets().iterator();
        while(allPlanetsInSystem.hasNext())
        {
            PlanetAPI planet = (PlanetAPI) allPlanetsInSystem.next();
            if (!planet.isStar() && planet.getOrbitFocus().equals(hostToken) && planet.getCircularOrbitRadius() < (hostToken.getRadius() + 500f))
            {
                return false;
            }
        }
        allPlanetsInSystem = null;

        //check if the host market and other planets are orbiting the same focus are too close to each other
        allPlanetsInSystem = this.market.getStarSystem().getPlanets().iterator();
        while(allPlanetsInSystem.hasNext())
        {
            PlanetAPI planet = (PlanetAPI)allPlanetsInSystem.next();
            if (!planet.isStar() && planet.getOrbitFocus().equals(hostToken.getOrbitFocus()))
            {
                if(Math.abs(planet.getCircularOrbitRadius() - hostToken.getCircularOrbitRadius()) < 600f && Math.abs(planet.getCircularOrbitRadius() - hostToken.getCircularOrbitRadius()) != 0)
                {
                    return false;
                }
            }
        }

        return true;
    }

    private int getNumAbandonedAstropoli(MarketAPI market)
    {
        int numAbandonedAstropoli = 0;
        Iterator allEntitiesInSystem = market.getStarSystem().getAllEntities().iterator();

        while(allEntitiesInSystem.hasNext())
        {
            SectorEntityToken entity = (SectorEntityToken)allEntitiesInSystem.next();
            if (entity.getOrbitFocus() != null && entity.getOrbitFocus().equals(market.getPrimaryEntity()) && entity.getId().contains("boggled_astropolis") && entity.getFaction().getId().equals("neutral"))
            {
                numAbandonedAstropoli++;
            }
        }

        return numAbandonedAstropoli;
    }

    @Override
    public boolean isAvailableToBuild()
    {
        int numAstro = numAstroInOrbit();
        int numAbandonedAstro = getNumAbandonedAstropoli(this.market);

        if(numAstro - numAbandonedAstro < 3 && !this.market.getPrimaryEntity().hasTag("station") && astropolisOrbitOK())
        {
            return true;
        }
        else
        {
            return false;
        }
    }

    @Override
    public boolean showWhenUnavailable()
    {
        if(this.market.getPrimaryEntity().hasTag("station"))
        {
            return false;
        }

        return true;
    }

    @Override
    public String getUnavailableReason()
    {
        int numAstro = numAstroInOrbit();
        int numAbandonedAstro = getNumAbandonedAstropoli(this.market);
        if(numAstro - numAbandonedAstro >= 3)
        {
            return "Each world can support a maximum of three astropoli.";
        }

        SectorEntityToken hostToken = this.market.getPrimaryEntity();

        //check if the host market radius is too small
        if(hostToken.getRadius() < 125f)
        {
            return "This world is too small to host an astropolis.";
        }

        if(!astropolisOrbitOK())
        {
            return "An astropolis would be unable to achieve a satisfactory orbit around this world due to other nearby celestial bodies.";
        }

        return "ERROR. TELL BOGGLED ABOUT THIS ON THE FORUMS.";
    }
}
