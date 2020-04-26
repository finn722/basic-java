package com.kkb.spring.test;

import java.io.InputStream;
import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.dom4j.Document;
import org.dom4j.DocumentException;
import org.dom4j.Element;
import org.dom4j.io.SAXReader;
import org.junit.Test;

import com.kkb.spring.ioc.BeanDefinition;
import com.kkb.spring.ioc.PropertyValue;
import com.kkb.spring.ioc.RuntimeBeanReference;
import com.kkb.spring.ioc.TypedStringValue;
import com.kkb.spring.po.User;
import com.kkb.spring.service.UserService;

public class TestSpringV2222 {
	private Map<String, BeanDefinition> beanDefinitions = new HashMap<>();
	/**
	 * 专门存储单例bean实例的集合
	 */
	private Map<String, Object> singletonObjects = new HashMap<String, Object>();

	@Test
	public void test() throws Exception {
		// 读取XML中的配置信息，封装成BeanDefinition，并且注册到Map集合中
		loadAndregisterBeanDefinitions("beans.xml");
		// 获取UserService的bean实例
		UserService service = (UserService) getBean("userService");
		// 调用UserService的方法
		User user = new User();
		user.setUsername("王五");
		List<User> users = service.queryUsers(user);

		System.out.println("结果：" + users);
	}

	private Object getBean(String name) {
		// 从缓存中获取要找的对象
		Object singleton = this.singletonObjects.get(name);
		if (singleton != null) {
			return singleton;
		}
		// 找不到，则获取指定名称的BeanDefinition对象
		// 此处使用到的就是抽象模板方法，我此处只定流程，不去实现，我也不懂如何实现，这不是我干的事情
		BeanDefinition bd = this.beanDefinitions.get(name);

		// 根据BeanDefinition中的信息，判断是单例还是多例（原型）
		if (bd.isSingleton()) { // 单例
			// 根据BeanDefinition对象，完成bean的创建
			singleton = createBean(bd);
			// 缓存已经创建的单例bean实例
			this.singletonObjects.put(name, singleton);
		} else if (bd.isPrototype()) {// 原型
			// 根据BeanDefinition对象，完成bean的创建
			singleton = createBean(bd);

		}
		return singleton;
	}

	private Object createBean(BeanDefinition bd) {
		String clazzName = bd.getClazzName();
		Class<?> clazz = resolveClassName(clazzName);
		if (clazz == null) {
			return null;
		}
		// 实例化bean
		// 注意：此时只是new了一个空对象
		Object sinleton = createBeanInstance(clazz);
		// bean的属性填充
		populateBean(sinleton, bd);

		// 初始化bean
		initBean(sinleton, bd);
		return sinleton;
	}

	private void initBean(Object sinleton, BeanDefinition bd) {
		// TODO 完成Aware接口（标记接口）相关的处理,spring mvc代码会用到
		// 1. aware接口的处理

		// TODO BeanPostProcessor的前置方法执行
		initMethod(sinleton, bd);
		// TODO BeanPostProcessor的后置方法执行（AOP代理对象产生的入口）
	}

	private void initMethod(Object sinleton, BeanDefinition bd) {
		// TODO 完成InitializingBean接口（标记接口）的处理，调用的是afterPropertySet方法

		// 完成init-metho标签属性对应的方法调用
		invokeMethod(sinleton, bd.getInitMethod());
	}

	private void setProperty(Object beanInstance, String name, Object valueToUse) {
		try {
			Class<?> clazz = beanInstance.getClass();
			Field field = clazz.getDeclaredField(name);
			field.setAccessible(true);
			field.set(beanInstance, valueToUse);
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	private void invokeMethod(Object beanInstance, String initMethod) {
		try {
			if (initMethod == null || "".equals(initMethod)) {
				return;
			}
			Class<?> clazz = beanInstance.getClass();
			Method method = clazz.getDeclaredMethod(initMethod);
			// 设置允许访问私有方法和变量，此处也称之为暴力破解
			method.setAccessible(true);
			method.invoke(beanInstance);
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	private void populateBean(Object sinleton, BeanDefinition bd) {
		List<PropertyValue> propertyValues = bd.getPropertyValues();
		for (PropertyValue propertyValue : propertyValues) {
			// 获取属性名称
			String name = propertyValue.getName();
			// 未处理的value对象
			Object value = propertyValue.getValue();
			// 解决之后的value值
			Object valueToUse = null;

			if (value instanceof TypedStringValue) {
				TypedStringValue typedStringValue = (TypedStringValue) value;
				String stringValue = typedStringValue.getValue();
				// 获取参数的类型
				Class<?> targetType = typedStringValue.getTargetType();

				// TODO 建议使用策略模式进行优化
				if (targetType == Integer.class) {
					valueToUse = Integer.parseInt(stringValue);
				} else if (targetType == String.class) {
					valueToUse = stringValue;
				} else {
					// ....
				}

			} else if (value instanceof RuntimeBeanReference) {
				RuntimeBeanReference reference = (RuntimeBeanReference) value;

				// 递归获取指定名称的bean实例
				// TODO 此处可能会发送循环依赖问题
				valueToUse = getBean(reference.getRef());
			} else {
				// ....
			}

			// 利用反射去设置bean的属性
			setProperty(sinleton, name, valueToUse);
		}
	}

	private Object createBeanInstance(Class<?> clazz) {
		// TODO 可以根据bean标签的配置选择使用实例工厂去创建Bean
		// TODO 可以根据bean标签的配置选择使用静态工厂去创建Bean

		// 还可以选择使用我们的构造方法去创建Bean
		return createObject(clazz);
	}

	private Object createObject(Class<?> clazz, Object... args) {
		try {
			// TODO 可以根据输入参数获取指定构造参数的构造方法
			Constructor<?> constructor = clazz.getConstructor();
			// 默认调用无参构造进行对象的创建
			return constructor.newInstance(args);
		} catch (Exception e) {
			e.printStackTrace();
		}
		return null;
	}

	private Class<?> resolveClassName(String clazzName) {
		try {
			return Class.forName(clazzName);
		} catch (ClassNotFoundException e) {
			e.printStackTrace();
		}
		return null;
	}

	private void loadAndregisterBeanDefinitions(String location) {
		// 获取流对象
		InputStream inputStream = this.getClass().getClassLoader().getResourceAsStream(location);
		// 创建文档对象
		Document document = createDocument(inputStream);

		// 按照spring定义的标签语义去解析Document文档
		registerBeanDefinitions(document.getRootElement());
	}

	private Document createDocument(InputStream inputStream) {
		Document document = null;
		try {
			SAXReader reader = new SAXReader();
			document = reader.read(inputStream);
			return document;
		} catch (DocumentException e) {
			e.printStackTrace();
		}
		return null;
	}

	@SuppressWarnings("unchecked")
	public void registerBeanDefinitions(Element rootElement) {
		// 获取<bean>和自定义标签（比如mvc:interceptors）
		List<Element> elements = rootElement.elements();
		for (Element element : elements) {
			// 获取标签名称
			String name = element.getName();
			if (name.equals("bean")) {
				// 解析默认标签，其实也就是bean标签
				parseDefaultElement(element);
			} else {
				// 解析自定义标签，比如mvc:interceptors标签回去
				parseCustomElement(element);
			}
		}
	}

	@SuppressWarnings("unchecked")
	private void parseDefaultElement(Element beanElement) {
		try {
			if (beanElement == null)
				return;
			// 获取id属性
			String id = beanElement.attributeValue("id");

			// 获取name属性
			String name = beanElement.attributeValue("name");
			// 获取class属性
			String clazzName = beanElement.attributeValue("class");
			if (clazzName == null || "".equals(clazzName)) {
				return;
			}
			Class<?> clazzType = Class.forName(clazzName);

			// 获取init-method属性
			String initMethod = beanElement.attributeValue("init-method");
			// 获取scope属性
			String scope = beanElement.attributeValue("scope");
			scope = scope != null && !scope.equals("") ? scope : "singleton";

			String beanName = id == null ? name : id;
			beanName = beanName == null ? clazzType.getSimpleName() : beanName;
			// 创建BeanDefinition对象
			// 此次可以使用构建者模式进行优化
			BeanDefinition beanDefinition = new BeanDefinition(clazzName, beanName);
			beanDefinition.setInitMethod(initMethod);
			beanDefinition.setScope(scope);
			// 获取property子标签集合
			List<Element> propertyElements = beanElement.elements();
			for (Element propertyElement : propertyElements) {
				parsePropertyElement(beanDefinition, propertyElement);
			}

			// 注册BeanDefinition信息
			this.beanDefinitions.put(beanName, beanDefinition);
		} catch (ClassNotFoundException e) {
			e.printStackTrace();
		}
	}

	private void parsePropertyElement(BeanDefinition beanDefination, Element propertyElement) {
		if (propertyElement == null)
			return;

		// 获取name属性
		String name = propertyElement.attributeValue("name");
		// 获取value属性
		String value = propertyElement.attributeValue("value");
		// 获取ref属性
		String ref = propertyElement.attributeValue("ref");

		// 如果value和ref都有值，则返回
		if (value != null && !value.equals("") && ref != null && !ref.equals("")) {
			return;
		}

		/**
		 * PropertyValue就封装着一个property标签的信息
		 */
		PropertyValue pv = null;

		if (value != null && !value.equals("")) {
			// 因为spring配置文件中的value是String类型，而对象中的属性值是各种各样的，所以需要存储类型
			TypedStringValue typeStringValue = new TypedStringValue(value);

			Class<?> targetType = getTypeByFieldName(beanDefination.getClazzName(), name);
			typeStringValue.setTargetType(targetType);

			pv = new PropertyValue(name, typeStringValue);
			beanDefination.addPropertyValue(pv);
		} else if (ref != null && !ref.equals("")) {

			RuntimeBeanReference reference = new RuntimeBeanReference(ref);
			pv = new PropertyValue(name, reference);
			beanDefination.addPropertyValue(pv);
		} else {
			return;
		}
	}

	private void parseCustomElement(Element element) {
		String name = element.getName();
		System.out.println(name);
		if ("aop-config".equals(name)) {
			parseAopConfigElement(element);
		} else {
			// TODO
		}
	}

	@SuppressWarnings("unchecked")
	private void parseAopConfigElement(Element configElement) {
		List<Element> elements = configElement.elements();
		for (Element ele : elements) {
			String name = ele.getName();
			System.out.println(name);

			if ("aop-aspect".equals(name)) {
				parseAopAspectElement(ele);
			} else {
				// TODO
			}
		}
	}

	@SuppressWarnings("unchecked")
	private void parseAopAspectElement(Element aspectElement) {
		String ref = aspectElement.attributeValue("ref");
		List<Element> elements = aspectElement.elements();
		for (Element element : elements) {
			String name = element.getName();
			if ("aop-before".equals(name)) {
				parseAopBefore(element);
			}else if ("aop-after".equals(name)) {
				parseAopAfter(element);
			}
		}
	}

	private void parseAopAfter(Element element) {
		// TODO Auto-generated method stub
		
	}

	private void parseAopBefore(Element element) {
		// TODO Auto-generated method stub
		
	}

	private Class<?> getTypeByFieldName(String beanClassName, String name) {
		try {
			Class<?> clazz = Class.forName(beanClassName);
			Field field = clazz.getDeclaredField(name);
			return field.getType();
		} catch (Exception e) {
			e.printStackTrace();
		}
		return null;
	}
}
