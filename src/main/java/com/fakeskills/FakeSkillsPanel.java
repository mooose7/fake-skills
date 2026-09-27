package com.fakeskills;

import java.awt.Color;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.image.BufferedImage;

import javax.swing.ToolTipManager;

import net.runelite.client.ui.PluginPanel;
import net.runelite.client.util.ImageUtil;

public class FakeSkillsPanel extends PluginPanel
{
    // =====================================================
    // ORIGINAL ARTWORK SIZE
    // =====================================================

    private static final int ART_WIDTH = 1174;
    private static final int ART_HEIGHT = 1340;

    /*
     * RuneLite sidebar display size.
     */
    private static final int DISPLAY_WIDTH = 225;

    private static final int DISPLAY_HEIGHT =
            (int) Math.round(
                    ART_HEIGHT
                            * (DISPLAY_WIDTH / (double) ART_WIDTH)
            );

    // =====================================================
    // SIX ROW POSITIONS
    // =====================================================

    /*
     * These are our calibrated row positions.
     * Do not change these unless we deliberately
     * reposition the artwork later.
     */
    private static final int[] ROW_CENTERS =
            {
                    145,   // Bank Standing
                    355,   // Traveling
                    560,   // Yapping
                    765,   // Doubling Down
                    979,   // Praying
                    1193   // Exp Waste
            };

    // =====================================================
    // LEVEL NUMBER POSITIONS
    // =====================================================

    /*
     * Top number sits above/left of the slash.
     */
    private static final int TOP_LEVEL_RIGHT_X = 430;

    /*
     * Bottom number sits below/right of the slash.
     */
    private static final int BOTTOM_LEVEL_RIGHT_X = 500;

    private static final int TOP_NUMBER_Y_OFFSET = -27;
    private static final int BOTTOM_NUMBER_Y_OFFSET = 62;

    private static final int LEVEL_FONT_SIZE = 58;

    // =====================================================
    // XP BAR POSITIONS
    // =====================================================

    private static final int BAR_X = 570;
    private static final int BAR_WIDTH = 412;
    private static final int BAR_HEIGHT = 31;

    private static final int BAR_Y_OFFSET = 55;

    private static final int BAR_INSET_X = 8;
    private static final int BAR_INSET_Y = 7;

    // =====================================================
    // ARTWORK
    // =====================================================

    private final BufferedImage background;

    // =====================================================
    // LIVE SKILL DATA
    // =====================================================

    private int bankStandingLevel = 1;
    private double bankStandingXp = 0.0;

    private int travelingLevel = 1;
    private double travelingXp = 0.0;

    private int yappingLevel = 1;
    private double yappingXp = 0.0;

    private int doublingDownLevel = 1;
    private double doublingDownXp = 0.0;

    private int prayingLevel = 1;
    private double prayingXp = 0.0;

    private int expWasteLevel = 1;
    private double expWasteXp = 0.0;

    // =====================================================
    // HOVER DESCRIPTIONS
    // =====================================================

    private int hoveredRow = -1;

    private final String[] skillDescriptions =
            {
                    "Gain exp by standing near a bank",
                    "Gain exp while traveling, 2x with a friend!",
                    "Gain exp when chatting",
                    "Gain exp by repeating actions",
                    "Gain exp by using prayers",
                    "Gain exp by doing nothing"
            };

    // =====================================================
    // CONSTRUCTOR
    // =====================================================

    public FakeSkillsPanel()
    {
        background =
                ImageUtil.loadImageResource(
                        FakeSkillsPanel.class,
                        "/fakeskills/panel_background.png"
                );

        setOpaque(false);

        /*
         * Tell Swing/RuneLite that this component
         * provides tooltip text.
         */
        ToolTipManager
                .sharedInstance()
                .registerComponent(this);

        /*
         * Watch the mouse while it moves over
         * our Fake Skills panel.
         */
        addMouseMotionListener(
                new MouseAdapter()
                {
                    @Override
                    public void mouseMoved(MouseEvent event)
                    {
                        int row =
                                getHoveredRow(
                                        event.getY()
                                );

                        if (row != hoveredRow)
                        {
                            hoveredRow = row;
                            repaint();
                        }
                    }
                }
        );

        /*
         * Clear the hovered skill when the mouse
         * leaves our interface.
         */
        addMouseListener(
                new MouseAdapter()
                {
                    @Override
                    public void mouseExited(MouseEvent event)
                    {
                        hoveredRow = -1;
                    }
                }
        );

        setPreferredSize(
                new Dimension(
                        DISPLAY_WIDTH,
                        DISPLAY_HEIGHT
                )
        );

        setMinimumSize(
                new Dimension(
                        DISPLAY_WIDTH,
                        DISPLAY_HEIGHT
                )
        );
    }

    // =====================================================
    // PAINT
    // =====================================================

    @Override
    protected void paintComponent(Graphics graphics)
    {
        super.paintComponent(graphics);

        /*
         * Build the complete interface at the
         * original artwork resolution first.
         */
        BufferedImage completedPanel =
                new BufferedImage(
                        ART_WIDTH,
                        ART_HEIGHT,
                        BufferedImage.TYPE_INT_ARGB
                );

        Graphics2D art =
                completedPanel.createGraphics();

        art.setRenderingHint(
                RenderingHints.KEY_ANTIALIASING,
                RenderingHints.VALUE_ANTIALIAS_OFF
        );

        art.setRenderingHint(
                RenderingHints.KEY_TEXT_ANTIALIASING,
                RenderingHints.VALUE_TEXT_ANTIALIAS_OFF
        );

        // Draw the static interface artwork.
        art.drawImage(
                background,
                0,
                0,
                ART_WIDTH,
                ART_HEIGHT,
                null
        );

        // Draw all six live skills.
        drawSkill(
                art,
                0,
                bankStandingLevel,
                bankStandingXp
        );

        drawSkill(
                art,
                1,
                travelingLevel,
                travelingXp
        );

        drawSkill(
                art,
                2,
                yappingLevel,
                yappingXp
        );

        drawSkill(
                art,
                3,
                doublingDownLevel,
                doublingDownXp
        );

        drawSkill(
                art,
                4,
                prayingLevel,
                prayingXp
        );

        drawSkill(
                art,
                5,
                expWasteLevel,
                expWasteXp
        );

        art.dispose();

        /*
         * Scale the entire completed interface down
         * to RuneLite sidebar size.
         */
        Graphics2D g =
                (Graphics2D) graphics.create();

        g.setRenderingHint(
                RenderingHints.KEY_INTERPOLATION,
                RenderingHints.VALUE_INTERPOLATION_NEAREST_NEIGHBOR
        );

        g.drawImage(
                completedPanel,
                0,
                0,
                DISPLAY_WIDTH,
                DISPLAY_HEIGHT,
                null
        );

        g.dispose();
    }

    // =====================================================
    // DRAW ONE SKILL
    // =====================================================

    private void drawSkill(
            Graphics2D g,
            int row,
            int level,
            double xp
    )
    {
        int centerY =
                ROW_CENTERS[row];

        drawLevelPair(
                g,
                centerY,
                level
        );

        drawXpBar(
                g,
                centerY,
                level,
                xp
        );
    }

    // =====================================================
    // LEVEL NUMBERS
    // =====================================================

    private void drawLevelPair(
            Graphics2D g,
            int centerY,
            int level
    )
    {
        String text =
                Integer.toString(level);

        g.setFont(
                new Font(
                        "Dialog",
                        Font.BOLD,
                        LEVEL_FONT_SIZE
                )
        );

        FontMetrics metrics =
                g.getFontMetrics();

        int textWidth =
                metrics.stringWidth(text);

        /*
         * Top and bottom use separate horizontal
         * positions so they surround the slash.
         */
        int topX =
                TOP_LEVEL_RIGHT_X
                        - textWidth;

        int bottomX =
                BOTTOM_LEVEL_RIGHT_X
                        - textWidth;

        int topY =
                centerY
                        + TOP_NUMBER_Y_OFFSET;

        int bottomY =
                centerY
                        + BOTTOM_NUMBER_Y_OFFSET;

        // Black shadow.
        g.setColor(
                Color.BLACK
        );

        g.drawString(
                text,
                topX + 4,
                topY + 4
        );

        g.drawString(
                text,
                bottomX + 4,
                bottomY + 4
        );

        // Yellow number.
        g.setColor(
                new Color(
                        255,
                        230,
                        0
                )
        );

        g.drawString(
                text,
                topX,
                topY
        );

        g.drawString(
                text,
                bottomX,
                bottomY
        );
    }

    // =====================================================
    // XP BAR
    // =====================================================

    private void drawXpBar(
            Graphics2D g,
            int centerY,
            int level,
            double xp
    )
    {
        double progress =
                getLevelProgress(
                        level,
                        xp
                );

        int usableWidth =
                BAR_WIDTH
                        - (BAR_INSET_X * 2);

        int fillWidth =
                (int) Math.round(
                        usableWidth
                                * progress
                );

        if (fillWidth <= 0)
        {
            return;
        }

        int x =
                BAR_X
                        + BAR_INSET_X;

        int y =
                centerY
                        + BAR_Y_OFFSET
                        + BAR_INSET_Y;

        int height =
                BAR_HEIGHT
                        - (BAR_INSET_Y * 2);

        /*
         * Dark green lower layer.
         */
        g.setColor(
                new Color(
                        75,
                        125,
                        10
                )
        );

        g.fillRect(
                x,
                y,
                fillWidth,
                height
        );

        /*
         * Bright green upper layer.
         */
        if (height > 5)
        {
            g.setColor(
                    new Color(
                            155,
                            225,
                            25
                    )
            );

            g.fillRect(
                    x,
                    y,
                    fillWidth,
                    height - 5
            );
        }
    }

    // =====================================================
    // LEVEL PROGRESS
    // =====================================================

    private double getLevelProgress(
            int level,
            double xp
    )
    {
        if (level >= 120)
        {
            return 1.0;
        }

        /*
         * Fake Skills special rule:
         *
         * Level 99 remains level 99 until 200m XP.
         * At 200m it becomes level 120.
         */
        if (level >= 99)
        {
            double startXp =
                    13_034_431.0;

            double endXp =
                    200_000_000.0;

            return clamp(
                    (xp - startXp)
                            / (endXp - startXp)
            );
        }

        double startXp =
                getXpForLevel(
                        level
                );

        double nextXp =
                getXpForLevel(
                        level + 1
                );

        double range =
                nextXp - startXp;

        if (range <= 0.0)
        {
            return 0.0;
        }

        return clamp(
                (xp - startXp)
                        / range
        );
    }

    // =====================================================
    // STANDARD OSRS XP TABLE
    // =====================================================

    private double getXpForLevel(
            int targetLevel
    )
    {
        if (targetLevel <= 1)
        {
            return 0.0;
        }

        int points = 0;

        for (
                int level = 1;
                level < targetLevel;
                level++
        )
        {
            points +=
                    Math.floor(
                            level
                                    + 300.0
                                    * Math.pow(
                                    2.0,
                                    level / 7.0
                            )
                    );
        }

        return Math.floor(
                points / 4.0
        );
    }

    private double clamp(
            double value
    )
    {
        if (value < 0.0)
        {
            return 0.0;
        }

        if (value > 1.0)
        {
            return 1.0;
        }

        return value;
    }

    // =====================================================
    // HOVER TOOLTIP
    // =====================================================

    @Override
    public String getToolTipText(MouseEvent event)
    {
        int row =
                getHoveredRow(
                        event.getY()
                );

        if (
                row >= 0
                        && row < skillDescriptions.length
        )
        {
            return skillDescriptions[row];
        }

        return null;
    }

    private int getHoveredRow(int mouseY)
    {
        /*
         * Convert the mouse position into the same
         * original-artwork coordinate system used
         * by our row positions.
         */
        double scale =
                ART_HEIGHT
                        / (double) DISPLAY_HEIGHT;

        int artworkY =
                (int) Math.round(
                        mouseY * scale
                );

        /*
         * Each row occupies the space halfway
         * between its neighboring row centers.
         */
        int firstBoundary =
                (ROW_CENTERS[0] + ROW_CENTERS[1]) / 2;

        if (artworkY < firstBoundary)
        {
            return 0;
        }

        for (
                int row = 1;
                row < ROW_CENTERS.length - 1;
                row++
        )
        {
            int upperBoundary =
                    (ROW_CENTERS[row - 1]
                            + ROW_CENTERS[row]) / 2;

            int lowerBoundary =
                    (ROW_CENTERS[row]
                            + ROW_CENTERS[row + 1]) / 2;

            if (
                    artworkY >= upperBoundary
                            && artworkY < lowerBoundary
            )
            {
                return row;
            }
        }

        int lastBoundary =
                (
                        ROW_CENTERS[
                                ROW_CENTERS.length - 2
                                ]
                                + ROW_CENTERS[
                                ROW_CENTERS.length - 1
                                ]
                ) / 2;

        if (
                artworkY >= lastBoundary
                        && artworkY <= ART_HEIGHT
        )
        {
            return ROW_CENTERS.length - 1;
        }

        return -1;
    }

    // =====================================================
    // LIVE SKILL UPDATES
    // =====================================================

    public void updateBankStanding(
            int level,
            double xp
    )
    {
        bankStandingLevel = level;
        bankStandingXp = xp;
        repaint();
    }

    public void updateTraveling(
            int level,
            double xp
    )
    {
        travelingLevel = level;
        travelingXp = xp;
        repaint();
    }

    public void updateYapping(
            int level,
            double xp
    )
    {
        yappingLevel = level;
        yappingXp = xp;
        repaint();
    }

    public void updateDoublingDown(
            int level,
            double xp
    )
    {
        doublingDownLevel = level;
        doublingDownXp = xp;
        repaint();
    }

    public void updatePraying(
            int level,
            double xp
    )
    {
        prayingLevel = level;
        prayingXp = xp;
        repaint();
    }

    public void updateExpWaste(
            int level,
            double xp
    )
    {
        expWasteLevel = level;
        expWasteXp = xp;
        repaint();
    }
}