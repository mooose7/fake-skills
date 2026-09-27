package com.fakeskills;

public class FakeSkillData
{
    private static final double MAX_XP = 200_000_000.0;

    private double xp;

    public FakeSkillData()
    {
        xp = 0.0;
    }

    public double getXp()
    {
        return xp;
    }

    public void setXp(double xp)
    {
        if (xp < 0)
        {
            this.xp = 0.0;
        }
        else if (xp > MAX_XP)
        {
            this.xp = MAX_XP;
        }
        else
        {
            this.xp = xp;
        }
    }

    public void addXp(double amount)
    {
        if (amount <= 0)
        {
            return;
        }

        xp += amount;

        if (xp > MAX_XP)
        {
            xp = MAX_XP;
        }
    }

    public int getLevel()
    {
        /*
         * Fake Skills special rule:
         *
         * Normal RuneScape XP curve through Level 99.
         *
         * Level stays at 99 after 13,034,431 XP.
         *
         * At exactly 200,000,000 XP:
         * Level 120.
         */

        if (xp >= MAX_XP)
        {
            return 120;
        }

        int points = 0;
        int level = 1;

        for (int lvl = 1; lvl < 99; lvl++)
        {
            points += Math.floor(
                    lvl + 300.0 * Math.pow(2.0, lvl / 7.0)
            );

            int requiredXp =
                    (int) Math.floor(points / 4.0);

            if (xp >= requiredXp)
            {
                level = lvl + 1;
            }
            else
            {
                break;
            }
        }

        return level;
    }
}