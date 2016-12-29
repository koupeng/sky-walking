package com.a.eye.skywalking.agent;

import com.a.eye.skywalking.agent.junction.SkyWalkingEnhanceMatcher;
import com.a.eye.skywalking.conf.Config;
import com.a.eye.skywalking.conf.ConfigInitializer;
import com.a.eye.skywalking.logging.EasyLogResolver;
import com.a.eye.skywalking.logging.api.ILog;
import com.a.eye.skywalking.logging.api.LogManager;
import com.a.eye.skywalking.plugin.AbstractClassEnhancePluginDefine;
import com.a.eye.skywalking.plugin.PluginBootstrap;
import com.a.eye.skywalking.plugin.PluginDefineCategory;
import com.a.eye.skywalking.plugin.PluginException;
import net.bytebuddy.agent.builder.AgentBuilder;
import net.bytebuddy.description.NamedElement;
import net.bytebuddy.description.type.TypeDescription;
import net.bytebuddy.dynamic.DynamicType;
import net.bytebuddy.matcher.ElementMatcher;
import net.bytebuddy.utility.JavaModule;

import java.io.File;
import java.lang.instrument.Instrumentation;
import java.net.URL;

import static net.bytebuddy.matcher.ElementMatchers.isInterface;
import static net.bytebuddy.matcher.ElementMatchers.not;

public class SkyWalkingAgent {
    static {
        LogManager.setLogResolver(new EasyLogResolver());
    }

    private static ILog logger;

    /**
     * 作为JavaAgent代理运行
     * 该方法在main方法之前运行，与main方法运行在同一个JVM中
     * 被同一个System ClassLoader加载
     * 被统一的安全策略和上下文管理
     * 用法：java -javaagent:JAVAAGENT1 -javaagent:JAVAAGENT2 -jar main.jar
     * @param agentArgs
     * @param instrumentation
     * @throws PluginException
     */
    public static void premain(String agentArgs, Instrumentation instrumentation) throws PluginException {
        logger = LogManager.getLogger(SkyWalkingAgent.class);

        initConfig();

        final PluginDefineCategory pluginDefineCategory = PluginDefineCategory.category(new PluginBootstrap().loadPlugins());

        new AgentBuilder.Default().type(enhanceClassMatcher(pluginDefineCategory).and(not(isInterface()))).transform(new AgentBuilder.Transformer() {
            public DynamicType.Builder<?> transform(DynamicType.Builder<?> builder, TypeDescription typeDescription, ClassLoader classLoader) {
                AbstractClassEnhancePluginDefine pluginDefine = pluginDefineCategory.findPluginDefine(typeDescription.getTypeName());
                return pluginDefine.define(typeDescription.getTypeName(), builder);
            }
        }).with(new AgentBuilder.Listener() {
            @Override
            public void onTransformation(TypeDescription typeDescription, ClassLoader classLoader, JavaModule module, DynamicType dynamicType) {

            }

            @Override
            public void onIgnored(TypeDescription typeDescription, ClassLoader classLoader, JavaModule module) {
            }

            @Override
            public void onError(String typeName, ClassLoader classLoader, JavaModule module, Throwable throwable) {
                logger.error("Failed to enhance class " + typeName, throwable);
            }

            @Override
            public void onComplete(String typeName, ClassLoader classLoader, JavaModule module) {
            }
        }).installOn(instrumentation);
    }


    private static <T extends NamedElement> ElementMatcher.Junction<T> enhanceClassMatcher(PluginDefineCategory pluginDefineCategory) {
        return new SkyWalkingEnhanceMatcher<T>(pluginDefineCategory);
    }

    private static String generateLocationPath() {
        return SkyWalkingAgent.class.getName().replaceAll("\\.", "/") + ".class";
    }


    private static void initConfig() {
        Config.SkyWalking.IS_PREMAIN_MODE = true;
        Config.SkyWalking.AGENT_BASE_PATH = initAgentBasePath();

        ConfigInitializer.initialize();
    }

    private static String initAgentBasePath() {
        try {
            String urlString = SkyWalkingAgent.class.getClassLoader().getSystemClassLoader().getResource(generateLocationPath()).toString();
            urlString = urlString.substring(urlString.indexOf("file:"), urlString.indexOf('!'));
            return new File(new URL(urlString).getFile()).getParentFile().getAbsolutePath();
        } catch (Exception e) {
            logger.error("Failed to init config .", e);
            return "";
        }
    }
}
