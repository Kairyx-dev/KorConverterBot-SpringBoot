package org.specter.converter.adapter.bot.configuration;

import net.dv8tion.jda.api.JDA;
import net.dv8tion.jda.api.JDABuilder;
import net.dv8tion.jda.api.OnlineStatus;
import net.dv8tion.jda.api.interactions.commands.OptionType;
import net.dv8tion.jda.api.interactions.commands.build.Commands;
import net.dv8tion.jda.api.requests.GatewayIntent;
import net.dv8tion.jda.api.requests.restaction.CommandListUpdateAction;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.specter.converter.adapter.bot.entity.BotCommand;
import org.specter.converter.adapter.bot.listener.CommandListener;
import org.specter.converter.adapter.bot.listener.MessageListener;
import org.specter.converter.adapter.bot.properties.BotProperties;
import org.specter.converter.application.port.input.AddIgnoreUserUseCase;
import org.specter.converter.application.port.input.CheckIgnoreUserUseCase;
import org.specter.converter.application.port.input.ConvertMessageUseCase;
import org.specter.converter.application.port.input.RemoveIgnoreUserUseCase;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.boot.info.BuildProperties;
import org.springframework.context.annotation.Bean;

@AutoConfiguration
@EnableConfigurationProperties(BotProperties.class)
public class BotAutoConfiguration {

    private static final Logger log = LoggerFactory.getLogger(BotAutoConfiguration.class);

    @Bean
    public MessageListener messageListener(
            ConvertMessageUseCase convertMessageUseCase, CheckIgnoreUserUseCase checkIgnoreUserUseCase) {
        return new MessageListener(convertMessageUseCase, checkIgnoreUserUseCase);
    }

    @Bean
    public CommandListener commandListener(
            AddIgnoreUserUseCase addIgnoreUserUseCase,
            RemoveIgnoreUserUseCase removeIgnoreUserUseCase,
            BuildProperties buildProperties) {
        return new CommandListener(addIgnoreUserUseCase, removeIgnoreUserUseCase, buildProperties);
    }

    @Bean
    public JDA jda(BotProperties botProperties, MessageListener messageListener, CommandListener commandListener) {
        log.info("build new jda instance");
        JDA jda = JDABuilder.createDefault(botProperties.token())
                .enableIntents(GatewayIntent.DIRECT_MESSAGES)
                .enableIntents(GatewayIntent.MESSAGE_CONTENT)
                .build();

        jda.getPresence().setStatus(OnlineStatus.ONLINE);
        jda.addEventListener(messageListener);
        jda.addEventListener(commandListener);

        CommandListUpdateAction commandUpdateAction = jda.updateCommands();

        commandUpdateAction
                .addCommands(
                        Commands.slash(BotCommand.ECO_TEST.getCommand(), BotCommand.ECO_TEST.getDescription())
                                .addOption(OptionType.STRING, "content", "봇이 따라할 대사입니다."),
                        Commands.slash(BotCommand.ECO_VERSION.getCommand(), BotCommand.ECO_VERSION.getDescription()),
                        Commands.slash(BotCommand.IGNORE_ME.getCommand(), BotCommand.IGNORE_ME.getDescription()),
                        Commands.slash(BotCommand.UN_IGNORE_ME.getCommand(), BotCommand.UN_IGNORE_ME.getDescription()))
                .queue();

        return jda;
    }
}
