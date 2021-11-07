package com.github.russiaplayer.music;

import com.github.russiaplayer.SQL.ServerSQL;
import com.github.russiaplayer.bot.Message;
import com.sedmelluq.discord.lavaplayer.player.AudioLoadResultHandler;
import com.sedmelluq.discord.lavaplayer.player.AudioPlayer;
import com.sedmelluq.discord.lavaplayer.player.AudioPlayerManager;
import com.sedmelluq.discord.lavaplayer.player.DefaultAudioPlayerManager;
import com.sedmelluq.discord.lavaplayer.source.AudioSourceManagers;
import com.sedmelluq.discord.lavaplayer.tools.FriendlyException;
import com.sedmelluq.discord.lavaplayer.track.AudioPlaylist;
import com.sedmelluq.discord.lavaplayer.track.AudioTrack;
import net.dv8tion.jda.api.entities.Guild;
import net.dv8tion.jda.api.entities.TextChannel;
import net.dv8tion.jda.api.utils.MarkdownSanitizer;

import java.util.HashMap;
import java.util.Map;

public class PlayerManager {
    private final Map<Long, GuildMusicManager> musicManager;
    private final AudioPlayerManager audioPlayerManager;
    private final ServerSQL sql;

    public PlayerManager(ServerSQL sql) {
        this.musicManager = new HashMap<>();
        this.audioPlayerManager = new DefaultAudioPlayerManager();
        this.sql = sql;

        AudioSourceManagers.registerRemoteSources(this.audioPlayerManager);
        AudioSourceManagers.registerLocalSource(this.audioPlayerManager);
    }

    public GuildMusicManager getMusicManger(Guild guild) {
        return this.musicManager.computeIfAbsent(guild.getIdLong(), (guildId) -> {
            final GuildMusicManager guildMusicManager = new GuildMusicManager(this.audioPlayerManager, guild, sql);

            guild.getAudioManager().setSendingHandler(guildMusicManager.getSendHandler());

            return guildMusicManager;
        });
    }

    public void loadAndPlay(TextChannel channel, String trackUrl){
        final GuildMusicManager musicManager = getMusicManger(channel.getGuild());
        Message message = new Message(channel.getGuild());

        audioPlayerManager.loadItemOrdered(musicManager, trackUrl, new AudioLoadResultHandler() {
            @Override
            public void trackLoaded(AudioTrack audioTrack) {
                musicManager.scheduler.queue(audioTrack);
                message.sendNormalMessage(channel.getIdLong(), "Adding: "
                        + MarkdownSanitizer.sanitize(audioTrack.getInfo().title)
                        + " by "
                        + MarkdownSanitizer.sanitize(audioTrack.getInfo().author));
            }

            @Override
            public void playlistLoaded(AudioPlaylist audioPlaylist) {
                for(AudioTrack track : audioPlaylist.getTracks()){
                    musicManager.scheduler.queue(track);
                }
                message.sendNormalMessage(channel.getIdLong(), "Adding: " + audioPlaylist.getTracks().size() + " to Playlist");
            }

            @Override
            public void noMatches() {
                message.sendNormalMessage(channel.getIdLong(), "No Match found");
            }

            @Override
            public void loadFailed(FriendlyException e) {
                System.out.println(e.toString());
                message.sendNormalMessage(channel.getIdLong(), "ERROR: " + e.toString());
            }
        });
    }
}