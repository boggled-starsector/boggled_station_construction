package data.campaign.econ.abilities;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.campaign.*;
import com.fs.starfarer.api.campaign.econ.CommoditySpecAPI;
import com.fs.starfarer.api.campaign.econ.Industry;
import com.fs.starfarer.api.campaign.econ.MarketAPI;
import com.fs.starfarer.api.campaign.econ.MarketConditionAPI;
import com.fs.starfarer.api.fleet.FleetMemberAPI;
import com.fs.starfarer.api.fleet.FleetMemberViewAPI;
import com.fs.starfarer.api.impl.campaign.abilities.BaseDurationAbility;
import com.fs.starfarer.api.impl.campaign.ids.Conditions;
import com.fs.starfarer.api.impl.campaign.ids.Industries;
import com.fs.starfarer.api.impl.campaign.submarkets.StoragePlugin;
import com.fs.starfarer.api.impl.campaign.terrain.AsteroidBeltTerrainPlugin;
import com.fs.starfarer.api.impl.campaign.terrain.BaseRingTerrain;
import com.fs.starfarer.api.ui.LabelAPI;
import com.fs.starfarer.api.ui.TooltipMakerAPI;
import com.fs.starfarer.api.util.Misc;
import java.awt.Color;
import java.util.Iterator;
import java.util.List;

public class Construct_Station extends BaseDurationAbility
{
    private float creditCost = 500000f;
    private float crewCost = 1000f;
    private float heavyMachineryCost = 250f;
    private float metalCost = 1000f;
    private float transplutonicsCost = 250f;

    public Construct_Station() { }

    private SectorEntityToken getFocusOfAsteroids()
    {
        Iterator allEntitiesInSystem = Global.getSector().getPlayerFleet().getStarSystem().getAllEntities().iterator();
        while(allEntitiesInSystem.hasNext())
        {
            SectorEntityToken entity = (SectorEntityToken)allEntitiesInSystem.next();
            if (entity instanceof CampaignTerrainAPI)
            {
                CampaignTerrainAPI terrain = (CampaignTerrainAPI)entity;
                String terrainID = terrain.getPlugin().getTerrainId();

                if(terrainID.equals("asteroid_belt") || terrainID.equals("asteroid_field"))
                {
                    if(terrain.getPlugin().containsEntity(Global.getSector().getPlayerFleet()))
                    {
                        return entity.getOrbitFocus();
                    }
                }
            }
        }

        return null;
    }

    private float calculateDistanceBetweenPoints(float x1, float y1, float x2, float y2)
    {
        return (float)Math.sqrt((y2 - y1) * (y2 - y1) + (x2 - x1) * (x2 - x1));
    }

    private float getAngle(float focusX, float focusY, float playerX, float playerY)
    {
        float angle = (float) Math.toDegrees(Math.atan2(focusY - playerY, focusX - playerX));

        //Not entirely sure what math is going on behind the scenes but this works to get the station to spawn next to the player
        angle = angle + 180f;

        return angle;
    }

    @Override
    protected void activateImpl()
    {
        CargoAPI playerCargo = Global.getSector().getPlayerFleet().getCargo();
        playerCargo.getCredits().subtract(creditCost);
        playerCargo.removeCommodity("metals", metalCost);
        playerCargo.removeCommodity("rare_metals", transplutonicsCost);
        playerCargo.removeCommodity("crew", crewCost);
        playerCargo.removeCommodity("heavy_machinery", heavyMachineryCost);

        CargoAPI cargo = null;
        SectorEntityToken targetToDelete = null;
        Iterator allEntitiesInSystem = Global.getSector().getPlayerFleet().getStarSystem().getAllEntities().iterator();
        while(allEntitiesInSystem.hasNext())
        {
            SectorEntityToken entity = (SectorEntityToken)allEntitiesInSystem.next();
            if(entity.hasTag("boggled_station_mining") && entity.getFaction().getId().equals("neutral"))
            {
                targetToDelete = entity;
            }
        }
        allEntitiesInSystem = null;

        StarSystemAPI system = Global.getSector().getPlayerFleet().getStarSystem();
        SectorEntityToken focus = getFocusOfAsteroids();
        float orbitRadius = calculateDistanceBetweenPoints(focus.getLocation().x, focus.getLocation().y, Global.getSector().getPlayerFleet().getLocation().x, Global.getSector().getPlayerFleet().getLocation().y);
        float orbitAngle = getAngle(focus.getLocation().x, focus.getLocation().y, Global.getSector().getPlayerFleet().getLocation().x, Global.getSector().getPlayerFleet().getLocation().y);

        SectorEntityToken newMiningStation = system.addCustomEntity("boggled_station_mining", system.getBaseName() + " Mining Station", "boggled_station_mining", Global.getSector().getPlayerFleet().getFaction().getId());
        newMiningStation.setCircularOrbitPointingDown(focus, orbitAngle + 3, orbitRadius, orbitRadius / 10.0F);
        newMiningStation.setInteractionImage("illustrations", "orbital_construction");

        //Create the mining station market
        MarketAPI market = Global.getFactory().createMarket("MiningStationMarket", newMiningStation.getName(), 3);
        market.setSize(3);

        market.setSurveyLevel(MarketAPI.SurveyLevel.FULL);
        market.setPrimaryEntity(newMiningStation);

        market.setFactionId(Global.getSector().getPlayerFleet().getFaction().getId());
        market.setPlayerOwned(true);

        market.addCondition(Conditions.POPULATION_3);
        market.addCondition(Conditions.ORE_MODERATE);
        market.addCondition(Conditions.RARE_ORE_MODERATE);

        market.addIndustry(Industries.POPULATION);
        market.addIndustry(Industries.SPACEPORT);

        newMiningStation.setMarket(market);

        Global.getSector().getEconomy().addMarket(market, true);

        //If the player doesn't view the colony management screen within a few days of market creation, then there can be a bug related to population growth
        Global.getSector().getCampaignUI().showInteractionDialog(newMiningStation);

        market.addSubmarket("storage");
        StoragePlugin storage = (StoragePlugin)market.getSubmarket("storage").getPlugin();
        storage.setPlayerPaidToUnlock(true);
        market.addSubmarket("local_resources");

        surveyAll(market);

        //Refreshes supply and demand for each industry on the market
        List<Industry> industries = market.getIndustries();
        for (int i = 0; i < industries.size(); i++)
        {
            industries.get(i).doPreSaveCleanup();
            industries.get(i).doPostSaveRestore();
        }

        if(targetToDelete != null)
        {
            cargo = targetToDelete.getMarket().getSubmarket("storage").getCargo();
            if(!cargo.isEmpty())
            {
                market.getSubmarket("storage").getCargo().addAll(cargo);
            }
            Global.getSector().getPlayerFleet().getStarSystem().removeEntity(targetToDelete);
        }
    }

    @Override
    public boolean isUsable()
    {
        if (Global.getSector().getPlayerFleet().isInHyperspace() || Global.getSector().getPlayerFleet().isInHyperspaceTransition())
        {
            return false;
        }

        boolean playerHasResources = true;
        boolean playerFleetInAsteroidBelt = false;
        boolean miningStationAlreadyExists = false;

        Iterator allEntitiesInSystem = Global.getSector().getPlayerFleet().getStarSystem().getAllEntities().iterator();
        while(allEntitiesInSystem.hasNext())
        {
            SectorEntityToken entity = (SectorEntityToken)allEntitiesInSystem.next();
            if (entity instanceof CampaignTerrainAPI)
            {
                CampaignTerrainAPI terrain = (CampaignTerrainAPI)entity;
                String terrainID = terrain.getPlugin().getTerrainId();

                if(terrainID.equals("asteroid_belt") || terrainID.equals("asteroid_field"))
                {
                    if(terrain.getPlugin().containsEntity(Global.getSector().getPlayerFleet()))
                    {
                        playerFleetInAsteroidBelt = true;
                        break;
                    }
                }
            }
        }
        allEntitiesInSystem = null;

        allEntitiesInSystem = Global.getSector().getPlayerFleet().getStarSystem().getAllEntities().iterator();
        while(allEntitiesInSystem.hasNext())
        {
            SectorEntityToken entity = (SectorEntityToken)allEntitiesInSystem.next();
            if(entity.hasTag("boggled_station_mining") && !entity.getFaction().getId().equals("neutral"))
            {
                miningStationAlreadyExists = true;
            }
        }

        CargoAPI playerCargo = Global.getSector().getPlayerFleet().getCargo();
        if(playerCargo.getCredits().get() < creditCost)
        {
            playerHasResources = false;
        }

        if(playerCargo.getCommodityQuantity("metals") < metalCost)
        {
            playerHasResources = false;
        }

        if(playerCargo.getCommodityQuantity("rare_metals") < transplutonicsCost)
        {
            playerHasResources = false;
        }

        if(playerCargo.getCommodityQuantity("crew") < crewCost)
        {
            playerHasResources = false;
        }

        if(playerCargo.getCommodityQuantity("heavy_machinery") < heavyMachineryCost)
        {
            playerHasResources = false;
        }

        return !this.isOnCooldown() && this.disableFrames <= 0 && playerFleetInAsteroidBelt && !miningStationAlreadyExists && playerHasResources;
    }

    @Override
    public boolean hasTooltip() {
        return true;
    }

    @Override
    public void createTooltip(TooltipMakerAPI tooltip, boolean expanded)
    {
        Color highlight = Misc.getHighlightColor();
        Color bad = Misc.getNegativeHighlightColor();

        LabelAPI title = tooltip.addTitle("Construct Station");
        float pad = 10.0F;
        tooltip.addPara("Construct a mining station in an asteroid belt. Expends %s credits, %s crew, %s heavy machinery, %s metals and %s transplutonics for construction.", pad, highlight, new String[]{(int)creditCost + "",(int)crewCost + "",(int)heavyMachineryCost +"", (int)metalCost + "", (int)transplutonicsCost +""});

        boolean playerFleetInAsteroidBelt = false;
        boolean miningStationAlreadyExists = false;

        if (!Global.getSector().getPlayerFleet().isInHyperspace() && !Global.getSector().getPlayerFleet().isInHyperspaceTransition())
        {
            Iterator allEntitiesInSystem = Global.getSector().getPlayerFleet().getStarSystem().getAllEntities().iterator();
            while(allEntitiesInSystem.hasNext())
            {
                SectorEntityToken entity = (SectorEntityToken)allEntitiesInSystem.next();
                if (entity instanceof CampaignTerrainAPI)
                {
                    CampaignTerrainAPI terrain = (CampaignTerrainAPI)entity;
                    String terrainID = terrain.getPlugin().getTerrainId();

                    if(terrainID.equals("asteroid_belt") || terrainID.equals("asteroid_field"))
                    {
                        if(terrain.getPlugin().containsEntity(Global.getSector().getPlayerFleet()))
                        {
                            playerFleetInAsteroidBelt = true;
                            break;
                        }
                    }
                }
            }
        }

        if (!Global.getSector().getPlayerFleet().isInHyperspace() && !Global.getSector().getPlayerFleet().isInHyperspaceTransition())
        {
            Iterator allEntitiesInSystem = Global.getSector().getPlayerFleet().getStarSystem().getAllEntities().iterator();
            while(allEntitiesInSystem.hasNext())
            {
                SectorEntityToken entity = (SectorEntityToken)allEntitiesInSystem.next();
                if(entity.hasTag("boggled_station_mining") && !entity.getFaction().getId().equals("neutral"))
                {
                    miningStationAlreadyExists = true;
                }
            }
        }

        if(!playerFleetInAsteroidBelt)
        {
            tooltip.addPara("Mining stations can only be built in an asteroid belt.", bad, pad);
        }

        if(miningStationAlreadyExists)
        {
            tooltip.addPara("Each system can only support one player-constructed mining station.", bad, pad);
        }

        CargoAPI playerCargo = Global.getSector().getPlayerFleet().getCargo();
        if(playerCargo.getCredits().get() < creditCost)
        {
            tooltip.addPara("Insufficient credits.", bad, pad);
        }

        if(playerCargo.getCommodityQuantity("crew") < crewCost)
        {
            tooltip.addPara("Insufficient crew.", bad, pad);
        }

        if(playerCargo.getCommodityQuantity("heavy_machinery") < heavyMachineryCost)
        {
            tooltip.addPara("Insufficient heavy machinery.", bad, pad);
        }

        if(playerCargo.getCommodityQuantity("metals") < metalCost)
        {
            tooltip.addPara("Insufficient metals.", bad, pad);
        }

        if(playerCargo.getCommodityQuantity("rare_metals") < transplutonicsCost)
        {
            tooltip.addPara("Insufficient transplutonics.", bad, pad);
        }
    }

    private void surveyAll(MarketAPI market)
    {
        for (MarketConditionAPI condition : market.getConditions())
        {
            condition.setSurveyed(true);
        }
    }

    @Override
    public boolean isTooltipExpandable() {
        return false;
    }

    @Override
    protected void applyEffect(float v, float v1) {

    }

    @Override
    protected void deactivateImpl() {

    }

    @Override
    protected void cleanupImpl() {

    }
}