package org.hidde2727.DiscordPlugin.Discord;

import net.dv8tion.jda.api.events.interaction.ModalInteractionEvent;
import net.dv8tion.jda.api.events.interaction.component.ButtonInteractionEvent;
import net.dv8tion.jda.api.events.interaction.component.StringSelectInteractionEvent;

/**
 * Can be used to subscribe to events from certain embeds/modals.
 */
public class EventListener {

    public void listenForEmbed(Embed embed) {
    }

    public void onButtonInteraction(ButtonInteractionEvent event) {
    }
    public void onModalInteraction(ModalInteractionEvent event) {}
    public void onStringSelectInteraction(StringSelectInteractionEvent event) {}
}
