package data.scripts;

//Requried for Example 1:
import com.fs.starfarer.api.BaseModPlugin;
import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.campaign.PlanetAPI;
import com.fs.starfarer.api.campaign.SectorAPI;
import com.fs.starfarer.api.campaign.SectorEntityToken;
import com.fs.starfarer.api.campaign.StarSystemAPI;
import com.fs.starfarer.api.impl.campaign.ids.Conditions;
import com.fs.starfarer.api.util.Misc;

//Additionally required for Example 2:
import com.fs.starfarer.api.campaign.PlanetSpecAPI;

// lazy lib
// import org.lazywizard.lazylib.campaign.MessageUtils;

/**
 * This mod demonstrates the bare-minimum necessary code/scripting required to add a planet to an existing star system.
 * It is intended for those with minimal programming experience, let alone Java experience.
 * It tries to get the reader started with the bare basics that they can use to make simple, though impactful,
 * changes to their StarSector game.
 *
 * At times, some programming jargon will be defined and then used. Even if it doesn't make sense, the reader should
 * still be able to duplicate the code herein and modify it to obtain their own desired results.
 *
 * TModPlugin.java is a "class" file. It will be compiled by the game (ie Java) into byte-code (an actual .class file).
 */
public class BoggledStationConstructionPlugin extends BaseModPlugin
{
    @Override
    public void onNewGame()
    {
        //Player has ability on new game start.
    }

    public void afterGameSave() {
        Global.getSector().getCharacterData().addAbility("boggled_construct_station");
    }

    public void beforeGameSave() {
        Global.getSector().getCharacterData().removeAbility("boggled_construct_station");
    }

    public void onGameLoad(boolean newGame)
    {
        if (!Global.getSector().getPlayerFleet().hasAbility("boggled_construct_station"))
        {
            Global.getSector().getCharacterData().addAbility("boggled_construct_station");
        }
    }
}