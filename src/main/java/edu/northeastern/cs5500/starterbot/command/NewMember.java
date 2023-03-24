package edu.northeastern.cs5500.starterbot.command;

import edu.northeastern.cs5500.starterbot.controller.UserController;
import java.util.Objects;
import javax.annotation.Nonnull;
import javax.inject.Inject;
import javax.inject.Singleton;
import lombok.extern.slf4j.Slf4j;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.entities.channel.concrete.TextChannel;
import net.dv8tion.jda.api.events.guild.member.GuildMemberJoinEvent;
import net.dv8tion.jda.api.events.interaction.component.StringSelectInteractionEvent;
import net.dv8tion.jda.api.interactions.components.selections.StringSelectMenu;
import net.dv8tion.jda.api.utils.messages.MessageCreateBuilder;

@Singleton
@Slf4j
public class NewMember implements NewMemberHandler, StringSelectHandler {
    private static final Integer EMBED_COLOR = 0x00FFFF;

    @Inject UserController userController;

    @Inject
    public NewMember() {
        // Defined public and empty for Dagger injection
    }

    @Override
    @Nonnull
    public String getName() {
        return "newmember";
    }

    @Override
    public void onGuildMemberJoin(@Nonnull GuildMemberJoinEvent event) {
        log.info("event: newmember");

        // Assigns the Guild ID to the user object when a user first joins
        userController.setGuildIdForUser(event.getUser().getId(), event.getGuild().getId());

        TextChannel textChannel =
                event.getGuild().getTextChannelsByName("welcome-channel", true).get(0);

        StringSelectMenu menu =
                StringSelectMenu.create("newmember")
                        .setPlaceholder("Select Your Preferred Currency")
                        .addOption("US dollar (USD)", "USD")
                        .addOption("Euro (EUR)", "EUR")
                        .addOption("Japanese yen (JPY)", "JPY")
                        .addOption("British pound sterling (GBP)", "GBP")
                        .addOption("Chinese renminbi (CNH)", "CNH")
                        .addOption("Australian dollar (AUD)", "AUD")
                        .addOption("Canadian dollar (CAD)", "CAD")
                        .addOption("Swiss franc (CHF)", "CHF")
                        .addOption("Hong Kong dollar (HKD)", "HKD")
                        .build();

        EmbedBuilder embedBuilder =
                new EmbedBuilder()
                        .setTitle(String.format("Welcome to %s!", event.getGuild().getName()))
                        .setDescription(
                                String.format(
                                        "Hello %s! For potential future sales and purchases, please select your preferred transactional currecny below.",
                                        event.getUser().getName()))
                        .setColor(EMBED_COLOR);

        MessageCreateBuilder messageCreateBuilder = new MessageCreateBuilder();
        messageCreateBuilder =
                messageCreateBuilder
                        .mention(event.getUser())
                        .addActionRow(menu)
                        .addEmbeds(embedBuilder.build());
        textChannel.sendMessage(messageCreateBuilder.build()).queue();
    }

    @Override
    public void onStringSelectInteraction(@Nonnull StringSelectInteractionEvent event) {
        final String response = event.getInteraction().getValues().get(0);
        Objects.requireNonNull(response);
        event.deferEdit()
                .setActionRow(
                        StringSelectMenu.create(getName())
                                .setPlaceholder(response)
                                .addOption(response, response)
                                .build()
                                .withDisabled(true))
                .queue();
        event.getUser()
                .openPrivateChannel()
                .complete()
                .sendMessage(String.format("%s has been set as your trading currency!", response))
                .queue();
        userController.setLocationOfResidence(event.getUser().getId(), response);
    }
}
