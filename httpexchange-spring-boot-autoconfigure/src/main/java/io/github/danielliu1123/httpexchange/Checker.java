package io.github.danielliu1123.httpexchange;

import static io.github.danielliu1123.httpexchange.Util.nameMatch;

import java.util.List;
import java.util.Set;
import lombok.experimental.UtilityClass;
import lombok.extern.slf4j.Slf4j;

/**
 * @author Freeman
 */
@Slf4j
@UtilityClass
class Checker {

    public static void checkUnusedConfig(HttpExchangeProperties properties) {
        // Identify the configuration items that are not taking effect and print warning messages.
        Set<Class<?>> classes = Cache.getClients().keySet();

        List<HttpExchangeProperties.Channel> channels = properties.getChannels();

        for (int i = 0; i < channels.size(); i++) {
            HttpExchangeProperties.Channel channel = channels.get(i);

            checkClassesConfiguration(classes, i, channel);

            checkClientsConfiguration(classes, i, channel);
        }
    }

    private static void checkClassesConfiguration(
            Set<Class<?>> classes, int i, HttpExchangeProperties.Channel channel) {
        int s = channel.getClasses().size();
        for (int j = 0; j < s; j++) {
            Class<?> clazz = channel.getClasses().get(j);
            if (classes.stream().noneMatch(clazz::isAssignableFrom)) {
                log.warn(
                        "The configuration item '{}.channels[{}].classes[{}]={}' doesn't take effect, please remove it!",
                        HttpExchangeProperties.PREFIX,
                        i,
                        j,
                        clazz.getCanonicalName());
            }
        }
    }

    private static void checkClientsConfiguration(
            Set<Class<?>> classes, int i, HttpExchangeProperties.Channel channel) {
        int size = channel.getClients().size();
        for (int j = 0; j < size; j++) {
            String name = channel.getClients().get(j);
            if (!nameMatch(name, classes)) {
                log.warn(
                        "The configuration item '{}.channels[{}].clients[{}]={}' doesn't take effect, please remove it!",
                        HttpExchangeProperties.PREFIX,
                        i,
                        j,
                        name);
            }
        }
    }
}
