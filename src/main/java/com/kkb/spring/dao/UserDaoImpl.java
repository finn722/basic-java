package com.kkb.spring.dao;

import java.io.IOException;
import java.io.InputStream;
import java.lang.reflect.Field;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.Properties;

import javax.sql.DataSource;

import com.kkb.spring.po.User;

public class UserDaoImpl implements UserDao {

	private DataSource dataSource;
	private Properties properties = new Properties();

	public void setDataSource(DataSource dataSource) {
		this.dataSource = dataSource;
		// V1版本测试时需要
		init();
	}

	// 初始化方法
	public void init() {
		if (!properties.isEmpty()) {
			return;
		}
		try {
			InputStream inputStream = this.getClass().getClassLoader().getResourceAsStream("sqlmapping.properties");
			properties.load(inputStream);
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	@Override
	public List<User> queryUserList(String sqlId, Object param) {
		Connection connection = null;
		PreparedStatement preparedStatement = null;
		ResultSet rs = null;

		try {
			connection = dataSource.getConnection();
			// 定义sql语句 ?表示占位符
			String sql = properties.getProperty("db.sql." + sqlId);

			System.out.println("SQL：" + sql);
			System.out.println("参数：" + param);
			// 获取预处理 statement
			preparedStatement = connection.prepareStatement(sql);

			// 设置参数，第一个参数为 sql 语句中参数的序号（从 1 开始），第二个参数为设置的
			if (param instanceof Integer) {
				// ...
			} else if (param instanceof String) {
				preparedStatement.setObject(1, param.toString());
			} else {
				Class<?> clazz = param.getClass();
				String params = properties.getProperty("db.sql." + sqlId + ".paramnames");
				String[] paramArray = params.split(",");
				for (int i = 0; i < paramArray.length; i++) {
					String name = paramArray[i];
					Field field = clazz.getDeclaredField(name);
					field.setAccessible(true);
					Object value = field.get(param);
					preparedStatement.setObject(i + 1, value);

				}
			}

			// 向数据库发出 sql 执行查询，查询出结果集
			rs = preparedStatement.executeQuery();

			// 遍历查询结果集
			List<User> results = new ArrayList<User>();

			Class<?> clazz = User.class;
			while (rs.next()) {
				User instance = (User) clazz.newInstance();
				ResultSetMetaData metaData = rs.getMetaData();
				int columnCount = metaData.getColumnCount();
				for (int i = 0; i < columnCount; i++) {
					String columnName = metaData.getColumnName(i + 1);

					Field field = clazz.getDeclaredField(columnName);
					field.setAccessible(true);

					field.set(instance, rs.getObject(i + 1));
				}

				results.add(instance);
			}

			return results;
		} catch (

		Exception e) {
			e.printStackTrace();
		} finally {
			// 释放资源
			if (rs != null) {
				try {
					rs.close();
				} catch (SQLException e) {
					e.printStackTrace();
				}
			}
			if (preparedStatement != null) {
				try {
					preparedStatement.close();
				} catch (SQLException e) {
					e.printStackTrace();
				}
			}
			if (connection != null) {
				try {
					connection.close();
				} catch (SQLException e) {
					// TODO Auto-generated catch block e.printStackTrace();
				}
			}
		}

		return null;
	}

}
