package edu.northeastern.cs5500.starterbot.command;

import edu.northeastern.cs5500.starterbot.command.handlers.ButtonHandler;
import edu.northeastern.cs5500.starterbot.command.handlers.SlashCommandHandler;
import edu.northeastern.cs5500.starterbot.controller.GuildController;
import edu.northeastern.cs5500.starterbot.controller.ListingController;
import edu.northeastern.cs5500.starterbot.controller.UserController;
import edu.northeastern.cs5500.starterbot.exceptions.ChannelNotFoundException;
import edu.northeastern.cs5500.starterbot.exceptions.GuildNotFoundException;
import edu.northeastern.cs5500.starterbot.model.Listing;
import java.util.ArrayList;
import java.util.List;
import javax.annotation.Nonnull;
import javax.inject.Inject;
import javax.inject.Singleton;
import lombok.extern.slf4j.Slf4j;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.JDA;
import net.dv8tion.jda.api.entities.User;
import net.dv8tion.jda.api.entities.channel.middleman.MessageChannel;
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent;
import net.dv8tion.jda.api.events.interaction.component.ButtonInteractionEvent;
import net.dv8tion.jda.api.interactions.commands.build.CommandData;
import net.dv8tion.jda.api.interactions.commands.build.Commands;
import net.dv8tion.jda.api.interactions.components.buttons.Button;
import net.dv8tion.jda.api.utils.messages.MessageCreateBuilder;
import net.dv8tion.jda.api.utils.messages.MessageCreateData;
import org.bson.types.ObjectId;

@Singleton
@Slf4j
public class MyListingsCommand implements SlashCommandHandler, ButtonHandler {

    private static final Integer EMBED_COLOR = 0x00FFFF;

    @Inject ListingController listingController;
    @Inject UserController userController;
    @Inject MessageBuilder messageBuilder;
    @Inject GuildController guildController;
    @Inject JDA jda;

    @Inject
    public MyListingsCommand() {
        // Defined public and empty for Dagger injection
    }

    @Override
    @Nonnull
    public String getName() {
        return "mylistings";
    }

    @Override
    @Nonnull
    public CommandData getCommandData() {
        return Commands.slash(getName(), "View all the listings that you have posted");
    }

    @Override
    public void onSlashCommandInteraction(@Nonnull SlashCommandInteractionEvent event) {
        log.info("event: /mylistings");

        var user = event.getUser();
        var discordUserId = user.getId();
        var discordDisplayName = user.getName();
        var guildId = event.getGuild().getId();
        var listingsMessages = getListingsMessages(discordUserId, discordDisplayName, guildId);

        if (listingsMessages.isEmpty()) {
            event.reply("No listings available").setEphemeral(true).complete();
            return;
        }

        sendListingsMessageToUser(user, listingsMessages);
        event.reply("Your listings has been sent to your DM").setEphemeral(true).complete();
    }

    /**
     * Sends the listing messages to user's DM.
     *
     * @param user - The user who intiated the command.
     * @param listingsMessages - The user's listings in message format.
     */
    private void sendListingsMessageToUser(User user, List<MessageCreateData> listingsMessages) {
        for (MessageCreateData message : listingsMessages) {
            messageBuilder.sendPrivateMessage(user, message);
        }
    }

    /**
     * Retrieves all listings in message format from the user.
     *
     * @param discordUserId - The user's id in discord.
     * @param discordDisplayName - The user's display name in discord.
     * @return List<MessageCreateBuilder>
     */
    private List<MessageCreateData> getListingsMessages(
            String discordUserId, @Nonnull String discordDisplayName, String guildId) {
        var listing = listingController.getListingsByMemberId(discordUserId, guildId);
        List<MessageCreateData> messages = new ArrayList<>();

        if (listing.isEmpty()) {
            return messages;
        }

        for (Listing list : listing) {

            var buttonId = String.format("%s:%s:delete", getName(), list.getId());
            var button = Button.danger(buttonId, "Delete");

            var messageCreateBuilder =
                    new MessageCreateBuilder()
                            .addActionRow(button)
                            .setEmbeds(messageBuilder.toMessageEmbed(list, discordDisplayName));
            messages.add(messageCreateBuilder.build());
        }
        return messages;
    }

    @Override
    public void onButtonInteraction(@Nonnull ButtonInteractionEvent event) {
        var buttonIds = event.getButton().getId().split(":");
        var listing = listingController.getListingById(new ObjectId(buttonIds[1]));

        onDeleteListingButtonClick(event, listing);
    }

    /**
     * Deletes listing in the trading channel and database when the "delete" button is clicked.
     *
     * @param event - the event of a button interaction
     * @param listing - the listing to be deleted.
     * @throws GuildNotFoundException - guild was not found in JDA.
     * @throws ChannelNotFoundException - text channel was not found in JDA.
     */
    void onDeleteListingButtonClick(@Nonnull ButtonInteractionEvent event, Listing listing) {
        var userId = event.getUser().getId();

        MessageChannel channel;
        try {
            channel = getTradingChannel(listing.getGuildId());
            var buttonEvent = event.deferEdit().setComponents();

            listingController.deleteListingById(listing.getId(), userId);
            channel.deleteMessageById(listing.getMessageId()).queue();

            buttonEvent
                    .setEmbeds(
                            new EmbedBuilder()
                                    .setDescription("Your post has been successfully deleted")
                                    .setColor(EMBED_COLOR)
                                    .build())
                    .queue();
        } catch (GuildNotFoundException e) {
            log.error(
                    "myListing Command encountered an exception when attempting to delete listing",
                    e);
            event.reply("Unable to remove listing because the server or channel no longer exists.")
                    .complete();
        } catch (ChannelNotFoundException e) {

        }
    }

    /**
     * Retrieves the trading channel where the listing is located.
     *
     * @param guildId - the id of the guild where the listing is located.
     * @return - The trading channel
     * @throws GuildNotFoundException - guild was not found in JDA.
     * @throws ChannelNotFoundException - text channel was not found in JDA.
     */
    MessageChannel getTradingChannel(@Nonnull String guildId)
            throws GuildNotFoundException, ChannelNotFoundException {
        var guild = jda.getGuildById(guildId);

        if (guild == null) {
            guildController.removeGuildByGuildId(guildId);
            throw new GuildNotFoundException("Guild ID no longer exists in JDA.");
        }

        var tradingChannelId = guildController.getGuildByGuildId(guildId).getTradingChannelId();
        var channel = guild.getTextChannelById(tradingChannelId);

        if (channel == null) {
            guildController.setTradingChannelId(guildId, null);
            throw new ChannelNotFoundException(
                    "Trading channel ID no longer exists in the specified guild in JDA.");
        }
        return channel;
    }
}