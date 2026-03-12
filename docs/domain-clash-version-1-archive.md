# Domain Clash Version 1 Archive

This file archives the pre-rework domain clash behavior and is not used by the mod at runtime.

## Version 1 flow

1. Both clash participants were locked in place for the entire clash.
2. They were repeatedly forced to face each other.
3. The client displayed the existing clash bar and a large center-screen prompt using `W`, `A`, `S`, or `D`.
4. Progress only came from entering the prompted key correctly.
5. Wrong inputs removed progress.
6. The first participant to reach a full bar won the clash and took over the domain.
7. Participants were invincible while the clash was active.

## Version 1 active config keys

- `domainClash.readyDelayTicks`
- `domainClash.promptCorrectProgressPercent`
- `domainClash.promptWrongPenaltyPercent`
- `domainClash.winProgressPercent`
- `domainClash.loserManaDrainPercent`
- `domainClash.loserCooldownMultiplier`
- `domainClash.particlesPerTick`
- `domainClash.splitPatternModulo`
- `domainClash.disableDomainEffectsDuringClash`
- `domainClash.forceLookAtOpponent`
- `domainClash.participantsInvincible`

## Version 2 replacement

The active implementation now uses a staged intro, on-screen instructions, free movement after the intro, and damage dealt to the opposing domain clasher as the only way to fill the clash bar.
