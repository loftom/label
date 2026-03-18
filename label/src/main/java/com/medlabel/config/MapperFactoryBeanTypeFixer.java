package com.medlabel.config;

import org.springframework.beans.BeansException;
import org.springframework.beans.factory.FactoryBean;
import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.beans.factory.config.BeanFactoryPostProcessor;
import org.springframework.beans.factory.config.ConfigurableListableBeanFactory;
import org.springframework.context.annotation.Configuration;
import org.springframework.util.ClassUtils;

@Configuration
public class MapperFactoryBeanTypeFixer implements BeanFactoryPostProcessor {

    @Override
    public void postProcessBeanFactory(ConfigurableListableBeanFactory beanFactory) throws BeansException {
        for (String name : beanFactory.getBeanDefinitionNames()) {
            BeanDefinition bd = beanFactory.getBeanDefinition(name);
            Object attr = bd.getAttribute(FactoryBean.OBJECT_TYPE_ATTRIBUTE);
            if (attr instanceof String) {
                try {
                    Class<?> cls = ClassUtils.forName((String) attr, this.getClass().getClassLoader());
                    bd.setAttribute(FactoryBean.OBJECT_TYPE_ATTRIBUTE, cls);
                } catch (ClassNotFoundException ignored) {
                }
            }
        }
    }
}
