/*
 * Copyright 2002-2016 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.springframework.amqp.rabbit.config;

import java.util.List;

import org.springframework.amqp.core.DirectExchange;
import org.springframework.amqp.core.FanoutExchange;
import org.springframework.amqp.core.HeadersExchange;
import org.springframework.amqp.core.Queue;
import org.springframework.amqp.core.TopicExchange;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.beans.factory.config.BeanDefinitionHolder;
import org.springframework.beans.factory.config.RuntimeBeanReference;
import org.springframework.beans.factory.config.TypedStringValue;
import org.springframework.beans.factory.support.BeanDefinitionBuilder;
import org.springframework.beans.factory.support.BeanDefinitionReaderUtils;
import org.springframework.beans.factory.support.ManagedMap;
import org.springframework.beans.factory.xml.AbstractSingleBeanDefinitionParser;
import org.springframework.beans.factory.xml.ParserContext;
import org.springframework.core.Conventions;
import org.springframework.util.StringUtils;
import org.springframework.util.xml.DomUtils;
import org.w3c.dom.Element;

/**
 * @author Dave Syer
 * @author Gary Russell
 * @author Artem Bilan
 */
class QbaoTemplateParser extends AbstractSingleBeanDefinitionParser {

	private static final String CONNECTION_FACTORY_ATTRIBUTE = "connection-factory";

	private static final String EXCHANGE_ATTRIBUTE = "exchange";

	private static final String QUEUE_ATTRIBUTE = "queue";

	private static final String ROUTING_KEY_ATTRIBUTE = "routing-key";

	private static final String RECEIVE_TIMEOUT_ATTRIBUTE = "receive-timeout";

	private static final String REPLY_TIMEOUT_ATTRIBUTE = "reply-timeout";

	private static final String MESSAGE_CONVERTER_ATTRIBUTE = "message-converter";

	private static final String ENCODING_ATTRIBUTE = "encoding";

	private static final String CHANNEL_TRANSACTED_ATTRIBUTE = "channel-transacted";

	private static final String REPLY_QUEUE_ATTRIBUTE = "reply-queue";

	private static final String REPLY_ADDRESS_ATTRIBUTE = "reply-address";

	private static final String USE_TEMPORARY_REPLY_QUEUES_ATTRIBUTE = "use-temporary-reply-queues";

	private static final String LISTENER_ELEMENT = "reply-listener";

	private static final String MANDATORY_ATTRIBUTE = "mandatory";

	private static final String RETURN_CALLBACK_ATTRIBUTE = "return-callback";

	private static final String CONFIRM_CALLBACK_ATTRIBUTE = "confirm-callback";

	private static final String CORRELATION_KEY = "correlation-key";

	private static final String RETRY_TEMPLATE = "retry-template";

	private static final String RECOVERY_CALLBACK = "recovery-callback";

	private static final String PATTERN_ATTRIBUTE = "pattern";//用于设置direct和topic类型的exchange，绑定queue的routingkey

	private static final String EXCHANGE_TYPE_ATTRIBUTE = "exchange-type";//exchange类型 fanout,direct,topic,headers

	private static final String KEY_ATTRIBUTE = "key";//exchange-type=headers 时此参数有效

	private static final String VALUE_ATTRIBUTE = "value";//exchange-type=headers 时此参数有效

	@Override
	protected Class<?> getBeanClass(Element element) {
		return RabbitTemplate.class;
	}

	@Override
	protected boolean shouldGenerateId() {
		return false;
	}

	@Override
	protected boolean shouldGenerateIdAsFallback() {
		return false;
	}

	@Override
	protected void doParse(Element element, ParserContext parserContext, BeanDefinitionBuilder builder) {
		String connectionFactoryRef = element.getAttribute(CONNECTION_FACTORY_ATTRIBUTE);

		if (!StringUtils.hasText(connectionFactoryRef)) {
			parserContext.getReaderContext().error("A '" + CONNECTION_FACTORY_ATTRIBUTE + "' attribute must be set.",
					element);
		}

		if (StringUtils.hasText(connectionFactoryRef)) {
			// Use constructor with connectionFactory parameter
			builder.addConstructorArgReference(connectionFactoryRef);
		}

		NamespaceUtils.setValueIfAttributeDefined(builder, element, CHANNEL_TRANSACTED_ATTRIBUTE);
		NamespaceUtils.setValueIfAttributeDefined(builder, element, QUEUE_ATTRIBUTE);
		NamespaceUtils.setValueIfAttributeDefined(builder, element, EXCHANGE_ATTRIBUTE);
		NamespaceUtils.setValueIfAttributeDefined(builder, element, ROUTING_KEY_ATTRIBUTE);
		NamespaceUtils.setValueIfAttributeDefined(builder, element, RECEIVE_TIMEOUT_ATTRIBUTE);
		NamespaceUtils.setValueIfAttributeDefined(builder, element, REPLY_TIMEOUT_ATTRIBUTE);
		NamespaceUtils.setValueIfAttributeDefined(builder, element, ENCODING_ATTRIBUTE);
		NamespaceUtils.setReferenceIfAttributeDefined(builder, element, MESSAGE_CONVERTER_ATTRIBUTE);
		String replyAddress = element.getAttribute(REPLY_ADDRESS_ATTRIBUTE);
		if (!StringUtils.hasText(replyAddress)) {
			NamespaceUtils.setReferenceIfAttributeDefined(builder, element, REPLY_QUEUE_ATTRIBUTE,
					Conventions.attributeNameToPropertyName(REPLY_ADDRESS_ATTRIBUTE));
		}
		NamespaceUtils.setValueIfAttributeDefined(builder, element, USE_TEMPORARY_REPLY_QUEUES_ATTRIBUTE);
		NamespaceUtils.setValueIfAttributeDefined(builder, element, REPLY_ADDRESS_ATTRIBUTE);
		NamespaceUtils.setReferenceIfAttributeDefined(builder, element, RETURN_CALLBACK_ATTRIBUTE);
		NamespaceUtils.setReferenceIfAttributeDefined(builder, element, CONFIRM_CALLBACK_ATTRIBUTE);
		NamespaceUtils.setValueIfAttributeDefined(builder, element, CORRELATION_KEY);
		NamespaceUtils.setReferenceIfAttributeDefined(builder, element, RETRY_TEMPLATE);
		NamespaceUtils.setReferenceIfAttributeDefined(builder, element, RECOVERY_CALLBACK);

		BeanDefinition expressionDef = NamespaceUtils.createExpressionDefinitionFromValueOrExpression(
				MANDATORY_ATTRIBUTE, "mandatory-expression", parserContext, element, false);
		if (expressionDef != null) {
			builder.addPropertyValue("mandatoryExpression", expressionDef);
		}

		BeanDefinition sendConnectionFactorySelectorExpression = NamespaceUtils
				.createExpressionDefIfAttributeDefined("send-connection-factory-selector-expression", element);
		if (sendConnectionFactorySelectorExpression != null) {
			builder.addPropertyValue("sendConnectionFactorySelectorExpression",
					sendConnectionFactorySelectorExpression);
		}

		BeanDefinition receiveConnectionFactorySelectorExpression = NamespaceUtils
				.createExpressionDefIfAttributeDefined("receive-connection-factory-selector-expression", element);
		if (receiveConnectionFactorySelectorExpression != null) {
			builder.addPropertyValue("receiveConnectionFactorySelectorExpression",
					receiveConnectionFactorySelectorExpression);
		}

		BeanDefinition userIdExpression = NamespaceUtils.createExpressionDefIfAttributeDefined("user-id-expression",
				element);
		if (userIdExpression != null) {
			builder.addPropertyValue("userIdExpression", userIdExpression);
		}

		BeanDefinition replyContainer = null;
		Element childElement = null;
		List<Element> childElements = DomUtils.getChildElementsByTagName(element, LISTENER_ELEMENT);
		if (childElements.size() > 0) {
			childElement = childElements.get(0);
		}
		if (childElement != null) {
			replyContainer = parseListener(childElement, element, parserContext);
			if (replyContainer != null) {
				replyContainer.getPropertyValues().add("messageListener",
						new RuntimeBeanReference(element.getAttribute(ID_ATTRIBUTE)));
				String replyContainerName = element.getAttribute(ID_ATTRIBUTE) + ".replyListener";
				parserContext.getRegistry().registerBeanDefinition(replyContainerName, replyContainer);
			}
		}
		if (replyContainer == null && element.hasAttribute(REPLY_QUEUE_ATTRIBUTE)) {
			parserContext.getReaderContext().error("For template '" + element.getAttribute(ID_ATTRIBUTE)
					+ "', when specifying a reply-queue, " + "a <reply-listener/> element is required", element);
		} else if (replyContainer != null && !element.hasAttribute(REPLY_QUEUE_ATTRIBUTE)) {
			parserContext.getReaderContext()
					.error("For template '" + element.getAttribute(ID_ATTRIBUTE)
							+ "', a <reply-listener/> element is not allowed if no " + "'reply-queue' is supplied",
							element);
		}

		/** ------------------注册queue和exchange start------------------------- */
		String queue = StringUtils.trimWhitespace(element.getAttribute(QUEUE_ATTRIBUTE));
		String exchange = StringUtils.trimWhitespace(element.getAttribute(EXCHANGE_ATTRIBUTE));
		String exchange_type = StringUtils.trimWhitespace(element.getAttribute(EXCHANGE_TYPE_ATTRIBUTE));
		String pattern = StringUtils.trimWhitespace(element.getAttribute(PATTERN_ATTRIBUTE));
		String key = StringUtils.trimWhitespace(element.getAttribute(KEY_ATTRIBUTE));
		String value = StringUtils.trimWhitespace(element.getAttribute(VALUE_ATTRIBUTE));

		// 注册queue beandefinition
		parseQueue(queue, parserContext);
		// 注册exchange BeanDefinition
		parseExchange(exchange, exchange_type, pattern, queue, key, value, parserContext);
		/** ------------------注册queue和exchange end------------------------- */
	}

	private BeanDefinition parseListener(Element childElement, Element element, ParserContext parserContext) {
		BeanDefinition replyContainer = RabbitNamespaceUtils.parseContainer(childElement, parserContext);
		if (replyContainer != null) {
			replyContainer.getPropertyValues().add("connectionFactory",
					new RuntimeBeanReference(element.getAttribute(CONNECTION_FACTORY_ATTRIBUTE)));
		}
		if (element.hasAttribute(REPLY_QUEUE_ATTRIBUTE)) {
			replyContainer.getPropertyValues().add("queues",
					new RuntimeBeanReference(element.getAttribute(REPLY_QUEUE_ATTRIBUTE)));
		}
		return replyContainer;
	}

	/**
	 * 注册queue的BeanDefinition
	 * 
	 * @param queueName
	 * @param parserContext
	 */
	private void parseQueue(String queueName, ParserContext parserContext) {
		if (StringUtils.hasText(queueName)) {
			BeanDefinitionBuilder builder = BeanDefinitionBuilder.genericBeanDefinition(Queue.class);
			builder.addConstructorArgValue(new TypedStringValue(queueName));
			BeanDefinition beanDefinition = builder.getBeanDefinition();
			BeanDefinitionHolder holder = new BeanDefinitionHolder(beanDefinition, queueName);
			BeanDefinitionReaderUtils.registerBeanDefinition(holder, parserContext.getRegistry());
		}
	}

	/**
	 * 按qbao-template 设置的类型注册对应的BeanDefinition
	 * 
	 * @param exchange
	 *            exchange名称
	 * @param exchangeType
	 *            exchange类型(fanout,direct,topic,headers)
	 * @param pattern
	 *            exchange绑定的queue的routingkey
	 * @param queue
	 *            queue名称
	 * @param key
	 *            key值
	 * @param value
	 *            key对应的value值
	 * @param parserContext
	 */
	private void parseExchange(String exchange, String exchangeType, String pattern, String queue, String key,
			String value, ParserContext parserContext) {
		if ("fanout".equals(exchangeType)) {
			BeanDefinitionBuilder builder = BeanDefinitionBuilder.genericBeanDefinition(FanoutExchange.class);
			doParseBindings(queue, parserContext, exchange, exchangeType, pattern, "", "");
			registerExchangeBeanDefinition(builder, exchange, parserContext);

		} else if ("direct".equals(exchangeType)||"".equals(exchangeType)) {
			BeanDefinitionBuilder builder = BeanDefinitionBuilder.genericBeanDefinition(DirectExchange.class);
			doParseBindings(queue, parserContext, exchange, exchangeType, pattern, "", "");
			registerExchangeBeanDefinition(builder, exchange, parserContext);

		} else if ("topic".equals(exchangeType)) {
			BeanDefinitionBuilder builder = BeanDefinitionBuilder.genericBeanDefinition(TopicExchange.class);
			doParseBindings(queue, parserContext, exchange, exchangeType, pattern, "", "");
			registerExchangeBeanDefinition(builder, exchange, parserContext);
		} else if ("headers".equals(exchangeType)) {
			BeanDefinitionBuilder builder = BeanDefinitionBuilder.genericBeanDefinition(HeadersExchange.class);
			doParseBindings(queue, parserContext, exchange, exchangeType, pattern, key, value);
			registerExchangeBeanDefinition(builder, exchange, parserContext);
		} else{
			parserContext.getReaderContext().error("<qbao-template> exchange-type must be in{fanout,direct,topic,headers}", "");
		}
	}

	/**
	 * 注册exchange BeanDefinition
	 * 
	 * @param builder
	 * @param exchange
	 * @param parserContext
	 */
	private void registerExchangeBeanDefinition(BeanDefinitionBuilder builder, String exchange,
			ParserContext parserContext) {
		builder.addConstructorArgValue(new TypedStringValue(exchange));
		BeanDefinition beanDefinition = builder.getBeanDefinition();
		BeanDefinitionHolder holder = new BeanDefinitionHolder(beanDefinition, exchange);
		BeanDefinitionReaderUtils.registerBeanDefinition(holder, parserContext.getRegistry());
	}

	private void doParseBindings(String queueName, ParserContext parserContext, String exchangeName,
			String exchangeType, String pattern, String key, String value) {
		if (StringUtils.hasText(queueName)) {
			BeanDefinitionBuilder bindingBuilder = BeanDefinitionBuilder
					.genericBeanDefinition(BindingFactoryBean.class);
			bindingBuilder.addPropertyValue("exchange", new TypedStringValue(exchangeName));
			parseDestination(queueName, "", parserContext, bindingBuilder);
			if ("topic".equals(exchangeType) || "direct".equals(exchangeType)) {
				bindingBuilder.addPropertyValue("routingKey", new TypedStringValue(pattern));
			} else if ("headers".equals(exchangeType)) {
				boolean hasKey = StringUtils.hasText(key);
				boolean hasValue = StringUtils.hasText(value);
				if (hasKey ^ hasValue) {
					parserContext.getReaderContext().error(
							"<qbao-template>  exchange-type=headers Both 'key/value' attributes have to be declared.",
							"");
				}
				if (!hasKey & !hasValue) {
					parserContext.getReaderContext().error(
							"<qbao-template>  exchange-type=headers 'key/value' attributes pair have to be declared.",
							"");
				}
				ManagedMap<TypedStringValue, TypedStringValue> map = new ManagedMap<TypedStringValue, TypedStringValue>();
				map.put(new TypedStringValue(key), new TypedStringValue(value));
				bindingBuilder.addPropertyValue("arguments", map);
			}
			BeanDefinition beanDefinition = bindingBuilder.getBeanDefinition();
			registerBeanDefinition(
					new BeanDefinitionHolder(beanDefinition,
							parserContext.getReaderContext().generateBeanName(beanDefinition)),
					parserContext.getRegistry());
		}
	}

	/**
	 * 设置exchange绑定的queue
	 * 
	 * @param queueAttribute
	 * @param exchangeAttribute
	 *            未扩展
	 * @param parserContext
	 * @param builder
	 */
	private void parseDestination(String queueAttribute, String exchangeAttribute, ParserContext parserContext,
			BeanDefinitionBuilder builder) {
		boolean hasQueueAttribute = StringUtils.hasText(queueAttribute);
		boolean hasExchangeAttribute = StringUtils.hasText(exchangeAttribute);
		if (!(hasQueueAttribute ^ hasExchangeAttribute)) {
			parserContext.getReaderContext().error("<qbao-template>  must have exactly one of 'queue' or 'exchange'",
					"");
		}
		if (hasQueueAttribute) {
			builder.addPropertyReference("destinationQueue", queueAttribute);
		}
		// if (hasExchangeAttribute) {
		// builder.addPropertyReference("destinationExchange",
		// exchangeAttribute);
		// }
	}

}
