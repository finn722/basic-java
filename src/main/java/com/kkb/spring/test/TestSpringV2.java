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
import org.junit.Before;
import org.junit.Test;

import com.kkb.spring.ioc.BeanDefinition;
import com.kkb.spring.ioc.PropertyValue;
import com.kkb.spring.ioc.RuntimeBeanReference;
import com.kkb.spring.ioc.TypedStringValue;
import com.kkb.spring.po.User;
import com.kkb.spring.service.UserService;

public class TestSpringV2 {

	// 只有单例的bean才需要放入Map集合中进行保存管理
	private Map<String, Object> singletonObjects = new HashMap<String, Object>();
	// 封装xml中的bean标签表示的信息
	private Map<String, BeanDefinition> beanDefinitions = new HashMap<String, BeanDefinition>();

	@Before
	public void init() {
		loadAndRegisterBeanDefinitions("beans.xml");
	}
	@Test
	public void test() throws Exception {

		UserService service = (UserService) getBean("userService");
		// 调用UserService的方法
		User user = new User();
		user.setUsername("王五");
		List<User> users = service.queryUsers(user);

		System.out.println("结果：" + users);
	}
	private void loadAndRegisterBeanDefinitions(String location) {
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
		// TODO Auto-generated method stub

	}

	private Object getBean(String beanName) {
		// 先去查询Map集合，如果有则直接返回
		Object singletonObject = singletonObjects.get(beanName);
		if (singletonObject != null) {
			return singletonObject;
		}
		// 如果没有，再去创建对象（单例的bean的创建、多例bean的创建）
		// ** 先去Map集合中获取指定beanName的bean的信息（BeanDefinition）
		BeanDefinition beanDefinition = this.beanDefinitions.get(beanName);
		if (beanDefinition == null) {
			return null;
		}
		// ** 根据BeanDefinition中的是否单例去分别创建单例Bean和多例Bean
		if (beanDefinition.isSingleton()) {
			singletonObject = createBean(beanDefinition);

			// 将创建的单例对象，放入Map集合中
			this.singletonObjects.put(beanName, singletonObject);
		} else if (beanDefinition.isPrototype()) {

			singletonObject = createBean(beanDefinition);
		}
		// 将创建好的对象放入到Map中并返回
		return singletonObject;
	}

	private Object createBean(BeanDefinition beanDefinition) {

		// 实例化 new
		Object bean = createInstance(beanDefinition);
		if (bean == null)
			return null;
		// 属性填充 set
		populateBean(bean, beanDefinition);
		// 初始化 init
		initBean(bean, beanDefinition);
		return bean;
	}

	private void initBean(Object bean, BeanDefinition beanDefinition) {
		// TODO Aware接口会在此时被处理

		invokeInitMethod(bean, beanDefinition);
	}

	private void invokeInitMethod(Object bean, BeanDefinition beanDefinition) {
		// bean标签配置了init-method属性
		String initMethod = beanDefinition.getInitMethod();
		if (initMethod == null || initMethod.equals("")) {
			return;
		}
		invokeMethod(bean, initMethod);
		// TODO bean标签实现了InitializingBean接口
	}

	private void invokeMethod(Object bean, String initMethod) {
		try {
			Class<?> clazz = bean.getClass();
			Method method = clazz.getDeclaredMethod(initMethod);
			method.invoke(bean);
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	private void populateBean(Object bean, BeanDefinition beanDefinition) {
		List<PropertyValue> propertyValues = beanDefinition.getPropertyValues();
		for (PropertyValue pv : propertyValues) {
			String name = pv.getName();
			Object value = pv.getValue();
			Object valueToUse = null;
			if (value instanceof TypedStringValue) {
				TypedStringValue typedStringValue = (TypedStringValue) value;
				String stringValue = typedStringValue.getValue();
				Class<?> targetType = typedStringValue.getTargetType();
				if (targetType == String.class) {
					valueToUse = stringValue;
				} else if (targetType == Integer.class) {
					valueToUse = Integer.parseInt(stringValue);
				} // ....
			} else if (value instanceof RuntimeBeanReference) {
				RuntimeBeanReference reference = (RuntimeBeanReference) value;
				String ref = reference.getRef();

				// 创建一个bean的时候，根据依赖注入情况，自动去创建另一个bean去注入
				valueToUse = getBean(ref);
			}
			setProperty(bean, name, valueToUse);
		}
	}

	private void setProperty(Object bean, String name, Object valueToUse) {
		try {
			Class<?> clazz = bean.getClass();
			Field field = clazz.getDeclaredField(name);
			field.setAccessible(true);
			field.set(bean, valueToUse);
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	private Object createInstance(BeanDefinition beanDefinition) {
		// TODO 可以通过静态工厂去创建？？？？
		// TODO 可以通过实例工厂去创建？？？？

		try {
			String clazzName = beanDefinition.getClazzName();
			Class<?> clazzType = resolveClass(clazzName);

			Constructor<?> constructor = clazzType.getDeclaredConstructor();
			Object bean = constructor.newInstance();
			return bean;
		} catch (Exception e) {
			e.printStackTrace();
		}
		return null;
	}

	private Class<?> resolveClass(String clazzName) {
		try {
			Class<?> clazz = Class.forName(clazzName);
			return clazz;
		} catch (ClassNotFoundException e) {
			e.printStackTrace();
		}
		return null;
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
