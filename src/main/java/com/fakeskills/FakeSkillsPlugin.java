package com.fakeskills;

import com.google.inject.Provides;

import java.awt.image.BufferedImage;

import javax.inject.Inject;

import net.runelite.api.Client;
import net.runelite.api.GameState;

import net.runelite.api.events.ChatMessage;
import net.runelite.api.events.GameStateChanged;
import net.runelite.api.events.GameTick;
import net.runelite.api.events.InteractingChanged;
import net.runelite.api.events.MenuOptionClicked;
import net.runelite.api.events.NpcDespawned;

import net.runelite.client.config.ConfigManager;
import net.runelite.client.eventbus.Subscribe;
import net.runelite.client.plugins.Plugin;
import net.runelite.client.plugins.PluginDescriptor;
import net.runelite.client.ui.ClientToolbar;
import net.runelite.client.ui.NavigationButton;
import net.runelite.client.util.ImageUtil;

@PluginDescriptor(
		name = "Fake Skills",
		description = "Adds a collection of passive fake skills to RuneLite."
)
public class FakeSkillsPlugin extends Plugin
{
	private static final String CONFIG_GROUP =
			"fakeskills";

	private static final String TRAVELING_XP_KEY =
			"travelingXp";

	private static final String BANK_STANDING_XP_KEY =
			"bankStandingXp";

	private static final String PRAYING_XP_KEY =
			"prayingXp";

	private static final String YAPPING_XP_KEY =
			"yappingXp";

	private static final String DOUBLING_DOWN_XP_KEY =
			"doublingDownXp";

	private static final String DOUBLING_DOWN_MONSTER_KEY =
			"doublingDownWaitingMonster";

	private static final String DOUBLING_DOWN_ACTION_KEY =
			"doublingDownWaitingAction";

	private static final String DOUBLING_DOWN_REWARD_KEY =
			"doublingDownDoubleRewardNext";

	private static final String EXP_WASTE_XP_KEY =
			"expWasteXp";

	@Inject
	private Client client;

	@Inject
	private ClientToolbar clientToolbar;

	@Inject
	private ConfigManager configManager;

	@Inject
	private FakeSkillsConfig config;

	private FakeSkillsPanel panel;
	private NavigationButton navButton;

	private final FakeSkillData traveling =
			new FakeSkillData();

	private TravelingTracker travelingTracker;

	private final FakeSkillData bankStanding =
			new FakeSkillData();

	private BankStandingTracker bankStandingTracker;

	private final FakeSkillData praying =
			new FakeSkillData();

	private PrayingTracker prayingTracker;

	private final FakeSkillData yapping =
			new FakeSkillData();

	private YappingTracker yappingTracker;

	private final FakeSkillData doublingDown =
			new FakeSkillData();

	private DoublingDownTracker doublingDownTracker;

	private final FakeSkillData expWaste =
			new FakeSkillData();

	private ExpWasteTracker expWasteTracker;

	@Override
	protected void startUp()
	{
		travelingTracker =
				new TravelingTracker(client);

		bankStandingTracker =
				new BankStandingTracker(client);

		prayingTracker =
				new PrayingTracker(client);

		yappingTracker =
				new YappingTracker(client);

		doublingDownTracker =
				new DoublingDownTracker(client);

		expWasteTracker =
				new ExpWasteTracker(client);

		loadTravelingXp();
		loadBankStandingXp();
		loadPrayingXp();
		loadYappingXp();
		loadDoublingDownXp();
		loadExpWasteXp();

		loadDoublingDownState();

		panel =
				new FakeSkillsPanel();

		updateTravelingDisplay();
		updateBankStandingDisplay();
		updatePrayingDisplay();
		updateYappingDisplay();
		updateDoublingDownDisplay();
		updateExpWasteDisplay();

		BufferedImage icon =
				ImageUtil.loadImageResource(
						FakeSkillsPlugin.class,
						"/fakeskills/icon.png"
				);

		navButton =
				NavigationButton.builder()
						.tooltip("Fake Skills")
						.icon(icon)
						.priority(5)
						.panel(panel)
						.build();

		clientToolbar.addNavigation(
				navButton
		);
	}

	@Override
	protected void shutDown()
	{
		saveTravelingXp();
		saveBankStandingXp();
		savePrayingXp();
		saveYappingXp();
		saveDoublingDownXp();
		saveExpWasteXp();

		saveDoublingDownState();

		if (travelingTracker != null)
		{
			travelingTracker.reset();
		}

		if (bankStandingTracker != null)
		{
			bankStandingTracker.reset();
		}

		if (prayingTracker != null)
		{
			prayingTracker.reset();
		}

		if (doublingDownTracker != null)
		{
			doublingDownTracker
					.clearTemporaryNpcTracking();
		}

		if (expWasteTracker != null)
		{
			expWasteTracker.reset();
		}

		if (navButton != null)
		{
			clientToolbar.removeNavigation(
					navButton
			);
		}

		panel = null;
		navButton = null;

		travelingTracker = null;
		bankStandingTracker = null;
		prayingTracker = null;
		yappingTracker = null;
		doublingDownTracker = null;
		expWasteTracker = null;
	}

	// =====================================================
	// GAME TICK
	// =====================================================

	@Subscribe
	public void onGameTick(GameTick event)
	{
		/*
		 * IMPORTANT:
		 *
		 * Exp Waste MUST run last.
		 *
		 * That way the other Fake Skills get the
		 * opportunity to tell it that Fake XP was
		 * earned during this tick.
		 */
		updateTraveling();
		updateBankStanding();
		updatePraying();
		updateDoublingDown();

		updateExpWaste();
	}

	// =====================================================
	// TRAVELING
	// =====================================================

	private void updateTraveling()
	{
		if (travelingTracker == null)
		{
			return;
		}

		if (!config.travelingEnabled())
		{
			travelingTracker.reset();
			return;
		}

		double xpEarned =
				travelingTracker.update();

		if (config.ironmanMode()
				&& client.getFollower() != null)
		{
			xpEarned /= 2.0;
		}

		if (xpEarned <= 0.0)
		{
			return;
		}

		traveling.addXp(xpEarned);

		notifyExpWasteOfFakeXp();

		saveTravelingXp();
		updateTravelingDisplay();
	}

	// =====================================================
	// BANK STANDING
	// =====================================================

	private void updateBankStanding()
	{
		if (bankStandingTracker == null)
		{
			return;
		}

		if (!config.bankStandingEnabled())
		{
			bankStandingTracker.reset();
			return;
		}

		double xpEarned =
				bankStandingTracker.update();

		if (xpEarned <= 0.0)
		{
			return;
		}

		bankStanding.addXp(xpEarned);

		notifyExpWasteOfFakeXp();

		saveBankStandingXp();
		updateBankStandingDisplay();
	}

	// =====================================================
	// PRAYING
	// =====================================================

	private void updatePraying()
	{
		if (prayingTracker == null)
		{
			return;
		}

		if (!config.prayingEnabled())
		{
			prayingTracker.reset();
			return;
		}

		double xpEarned =
				prayingTracker.update();

		if (xpEarned <= 0.0)
		{
			return;
		}

		praying.addXp(xpEarned);

		notifyExpWasteOfFakeXp();

		savePrayingXp();
		updatePrayingDisplay();
	}

	// =====================================================
	// DOUBLING DOWN
	// =====================================================

	private void updateDoublingDown()
	{
		if (doublingDownTracker == null)
		{
			return;
		}

		if (!config.doublingDownEnabled())
		{
			doublingDownTracker.clearTemporaryNpcTracking();
			return;
		}

		double xpEarned =
				doublingDownTracker.update();

		saveDoublingDownState();

		if (xpEarned <= 0.0)
		{
			return;
		}

		doublingDown.addXp(
				xpEarned
		);

		notifyExpWasteOfFakeXp();

		saveDoublingDownXp();
		updateDoublingDownDisplay();

	}

	// =====================================================
	// EXP WASTE
	// =====================================================

	private void updateExpWaste()
	{
		if (expWasteTracker == null)
		{
			return;
		}

		if (!config.expWasteEnabled())
		{
			expWasteTracker.reset();
			return;
		}

		double xpEarned =
				expWasteTracker.update();

		if (xpEarned <= 0.0)
		{
			return;
		}

		expWaste.addXp(
				xpEarned
		);

		saveExpWasteXp();
		updateExpWasteDisplay();

	}

	private void notifyExpWasteOfFakeXp()
	{
		if (expWasteTracker != null)
		{
			expWasteTracker
					.notifyFakeXpEarned();
		}
	}

	// =====================================================
	// YAPPING
	// =====================================================

	@Subscribe
	public void onChatMessage(
			ChatMessage event
	)
	{
		if (yappingTracker == null
				|| !config.yappingEnabled())
		{
			return;
		}

		double xpEarned =
				yappingTracker.processMessage(
						event
				);

		if (xpEarned <= 0.0)
		{
			return;
		}

		if (config.ironmanMode())
		{
			xpEarned *= 2.0;
		}

		yapping.addXp(
				xpEarned
		);

		/*
		 * Yapping happens through an event rather
		 * than GameTick, so notify Exp Waste here.
		 */
		notifyExpWasteOfFakeXp();

		saveYappingXp();
		updateYappingDisplay();

	}

	// =====================================================
	// MENU CLICKS
	// =====================================================

	@Subscribe
	public void onMenuOptionClicked(
			MenuOptionClicked event
	)
	{
		if (doublingDownTracker != null
				&& config.doublingDownEnabled())
		{
			doublingDownTracker
					.onMenuOptionClicked(
							event
					);

			saveDoublingDownState();
		}

		if (expWasteTracker != null
				&& config.expWasteEnabled())
		{
			expWasteTracker
					.onMenuOptionClicked(
							event
					);
		}
	}

	// =====================================================
	// COMBAT
	// =====================================================

	@Subscribe
	public void onInteractingChanged(
			InteractingChanged event
	)
	{
		if (doublingDownTracker != null
				&& config.doublingDownEnabled())
		{
			doublingDownTracker
					.onInteractingChanged(
							event
					);
		}
	}

	@Subscribe
	public void onNpcDespawned(
			NpcDespawned event
	)
	{
		if (doublingDownTracker != null
				&& config.doublingDownEnabled())
		{
			doublingDownTracker
					.onNpcDespawned(
							event
					);

			saveDoublingDownState();
		}
	}

	// =====================================================
	// GAME STATE
	// =====================================================

	@Subscribe
	public void onGameStateChanged(
			GameStateChanged event
	)
	{
		if (
				event.getGameState()
						!= GameState.LOGGED_IN
		)
		{
			if (travelingTracker != null)
			{
				travelingTracker.reset();
			}

			if (bankStandingTracker != null)
			{
				bankStandingTracker.reset();
			}

			if (prayingTracker != null)
			{
				prayingTracker.reset();
			}

			if (doublingDownTracker != null)
			{
				saveDoublingDownState();

				doublingDownTracker
						.clearTemporaryNpcTracking();
			}

			if (expWasteTracker != null)
			{
				expWasteTracker.reset();
			}
		}
	}

	// =====================================================
	// DISPLAY
	// =====================================================

	private void updateTravelingDisplay()
	{
		if (panel != null)
		{
			panel.updateTraveling(
					traveling.getLevel(),
					traveling.getXp()
			);
		}
	}

	private void updateBankStandingDisplay()
	{
		if (panel != null)
		{
			panel.updateBankStanding(
					bankStanding.getLevel(),
					bankStanding.getXp()
			);
		}
	}

	private void updatePrayingDisplay()
	{
		if (panel != null)
		{
			panel.updatePraying(
					praying.getLevel(),
					praying.getXp()
			);
		}
	}

	private void updateYappingDisplay()
	{
		if (panel != null)
		{
			panel.updateYapping(
					yapping.getLevel(),
					yapping.getXp()
			);
		}
	}

	private void updateDoublingDownDisplay()
	{
		if (panel != null)
		{
			panel.updateDoublingDown(
					doublingDown.getLevel(),
					doublingDown.getXp()
			);
		}
	}

	private void updateExpWasteDisplay()
	{
		if (panel != null)
		{
			panel.updateExpWaste(
					expWaste.getLevel(),
					expWaste.getXp()
			);
		}
	}

	// =====================================================
	// SAVE XP
	// =====================================================

	private void saveTravelingXp()
	{
		configManager.setConfiguration(
				CONFIG_GROUP,
				TRAVELING_XP_KEY,
				traveling.getXp()
		);
	}

	private void saveBankStandingXp()
	{
		configManager.setConfiguration(
				CONFIG_GROUP,
				BANK_STANDING_XP_KEY,
				bankStanding.getXp()
		);
	}

	private void savePrayingXp()
	{
		configManager.setConfiguration(
				CONFIG_GROUP,
				PRAYING_XP_KEY,
				praying.getXp()
		);
	}

	private void saveYappingXp()
	{
		configManager.setConfiguration(
				CONFIG_GROUP,
				YAPPING_XP_KEY,
				yapping.getXp()
		);
	}

	private void saveDoublingDownXp()
	{
		configManager.setConfiguration(
				CONFIG_GROUP,
				DOUBLING_DOWN_XP_KEY,
				doublingDown.getXp()
		);
	}

	private void saveExpWasteXp()
	{
		configManager.setConfiguration(
				CONFIG_GROUP,
				EXP_WASTE_XP_KEY,
				expWaste.getXp()
		);
	}

	// =====================================================
	// SAVE DOUBLING DOWN STATE
	// =====================================================

	private void saveDoublingDownState()
	{
		if (doublingDownTracker == null)
		{
			return;
		}

		String monsterName =
				doublingDownTracker
						.getWaitingMonsterName();

		if (monsterName == null)
		{
			configManager.unsetConfiguration(
					CONFIG_GROUP,
					DOUBLING_DOWN_MONSTER_KEY
			);
		}
		else
		{
			configManager.setConfiguration(
					CONFIG_GROUP,
					DOUBLING_DOWN_MONSTER_KEY,
					monsterName
			);
		}

		String action =
				doublingDownTracker
						.getWaitingAction();

		if (action == null)
		{
			configManager.unsetConfiguration(
					CONFIG_GROUP,
					DOUBLING_DOWN_ACTION_KEY
			);
		}
		else
		{
			configManager.setConfiguration(
					CONFIG_GROUP,
					DOUBLING_DOWN_ACTION_KEY,
					action
			);
		}

		configManager.setConfiguration(
				CONFIG_GROUP,
				DOUBLING_DOWN_REWARD_KEY,
				doublingDownTracker
						.isDoubleRewardNext()
		);
	}

	// =====================================================
	// LOAD XP
	// =====================================================

	private void loadTravelingXp()
	{
		loadXp(
				traveling,
				configManager.getConfiguration(
						CONFIG_GROUP,
						TRAVELING_XP_KEY
				)
		);
	}

	private void loadBankStandingXp()
	{
		loadXp(
				bankStanding,
				configManager.getConfiguration(
						CONFIG_GROUP,
						BANK_STANDING_XP_KEY
				)
		);
	}

	private void loadPrayingXp()
	{
		loadXp(
				praying,
				configManager.getConfiguration(
						CONFIG_GROUP,
						PRAYING_XP_KEY
				)
		);
	}

	private void loadYappingXp()
	{
		loadXp(
				yapping,
				configManager.getConfiguration(
						CONFIG_GROUP,
						YAPPING_XP_KEY
				)
		);
	}

	private void loadDoublingDownXp()
	{
		loadXp(
				doublingDown,
				configManager.getConfiguration(
						CONFIG_GROUP,
						DOUBLING_DOWN_XP_KEY
				)
		);
	}

	private void loadExpWasteXp()
	{
		loadXp(
				expWaste,
				configManager.getConfiguration(
						CONFIG_GROUP,
						EXP_WASTE_XP_KEY
				)
		);
	}

	// =====================================================
	// LOAD DOUBLING DOWN STATE
	// =====================================================

	private void loadDoublingDownState()
	{
		if (doublingDownTracker == null)
		{
			return;
		}

		String monsterName =
				configManager.getConfiguration(
						CONFIG_GROUP,
						DOUBLING_DOWN_MONSTER_KEY
				);

		doublingDownTracker
				.setWaitingMonsterName(
						monsterName
				);

		String action =
				configManager.getConfiguration(
						CONFIG_GROUP,
						DOUBLING_DOWN_ACTION_KEY
				);

		doublingDownTracker
				.setWaitingAction(
						action
				);

		String rewardState =
				configManager.getConfiguration(
						CONFIG_GROUP,
						DOUBLING_DOWN_REWARD_KEY
				);

		doublingDownTracker
				.setDoubleRewardNext(
						Boolean.parseBoolean(
								rewardState
						)
				);
	}

	// =====================================================
	// SHARED XP LOADER
	// =====================================================

	private void loadXp(
			FakeSkillData skill,
			String savedXp
	)
	{
		if (savedXp == null)
		{
			skill.setXp(0.0);
			return;
		}

		try
		{
			skill.setXp(
					Double.parseDouble(
							savedXp
					)
			);
		}
		catch (NumberFormatException ignored)
		{
			skill.setXp(0.0);
		}
	}

	@Provides
	FakeSkillsConfig provideConfig(
			ConfigManager configManager
	)
	{
		return configManager.getConfig(
				FakeSkillsConfig.class
		);
	}
}
