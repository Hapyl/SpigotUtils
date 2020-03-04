        // Registering a class in your onEnable()
        // like this:
        new CustomCraftingRecipe(this);

        // Then, to create simple recipe, use
        // this constructor.

        /** Recipe Creation */
        new CustomCraftingRecipe("recipe_id", // ID that must be different for every recipes.
                new ItemStack[]{
                        null, null, null, // [0][1][2]
                        null, null, null, // [3][4][5] -> Represents this slots in crafting table.
                        null, null, null  // [6][7][8]
                },
                new ItemStack(Material.STONE)) // The result of the recipe;
                .create();

        // Example for an apple recipe:
        // I'm using static 'toStack' method for replacement to 'new ItemStack()',
        // it's just faster, but you can use anything you want.

        // As the result, it's gonna be 2 apples!
        new CustomCraftingRecipe("apple",
                new ItemStack[]{
                        null, toStack(Material.BONE_MEAL), null,
                        null, toStack(Material.APPLE), null,
                        null, toStack(Material.BONE_MEAL), null
                },
                toStack(Material.APPLE, 2))
                .create();
        // Done, you now have a recipe of 2 apples, created using 1 apple and 2 bone meal.
        // [!] Shapeless recipes are currently not implemented. [!]

        /** Extra Recipe Parameters */

        // You can also add custom parameters to your recipe, such as:
        //      .withSound() -- Plays sound when item crafted.
        //      .addAlias() -- Adds another way of crafting the same item.
        //      .withPermission() -- Making recipe require a permission node in order to craft it.
        //      .withExpRequired() -- Requires player to have certain amount of Experience LVLs.
        //      .withChance() -- Adds chance of creating an item.
        //      .withMessage() -- Adds message when item crafted.

        // There is also:
        //      .hideItemRequirementsOnLore() -- Hides all requirements from the item lore (If there is such)
        //      .allowBulk() -- Allows an item to be created in bulk (Shift Click)

        // [!] Don't forget to add them before you .create() you item, and don't actually forget to .create() it!

        // There is more! But it's basics you need to know.
