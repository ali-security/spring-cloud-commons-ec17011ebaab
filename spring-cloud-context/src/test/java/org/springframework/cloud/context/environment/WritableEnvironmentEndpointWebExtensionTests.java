/*
 * Copyright 2012-2019 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.springframework.cloud.context.environment;

import java.util.Collections;

import org.junit.Test;

import org.springframework.context.ApplicationEventPublisher;
import org.springframework.mock.env.MockEnvironment;

import static org.assertj.core.api.Assertions.fail;
import static org.assertj.core.api.BDDAssertions.then;
import static org.mockito.Mockito.mock;
import static org.springframework.cloud.context.environment.WritableEnvironmentEndpointWebExtension.VALID_KEYS_REGEX_PROPERTY;

public class WritableEnvironmentEndpointWebExtensionTests {

	@Test
	public void writeRejectedWhenValidKeysRegexNotSet() {
		MockEnvironment environment = new MockEnvironment();
		WritableEnvironmentEndpointWebExtension extension = extension(environment);

		assertRejected(extension, "spring.cloud.bootstrap.location");
		then(environment.getProperty("spring.cloud.bootstrap.location")).isNull();
	}

	@Test
	public void writeRejectedWhenValidKeysRegexIsBlank() {
		MockEnvironment environment = new MockEnvironment()
				.withProperty(VALID_KEYS_REGEX_PROPERTY, " ");
		WritableEnvironmentEndpointWebExtension extension = extension(environment);

		assertRejected(extension, "message");
		then(environment.getProperty("message")).isNull();
	}

	@Test
	public void writeRejectedWhenKeyDoesNotFullyMatch() {
		MockEnvironment environment = new MockEnvironment()
				.withProperty(VALID_KEYS_REGEX_PROPERTY, "message");
		WritableEnvironmentEndpointWebExtension extension = extension(environment);

		assertRejected(extension, "xmessage");
		assertRejected(extension, "message.suffix");
		then(environment.getProperty("xmessage")).isNull();
		then(environment.getProperty("message.suffix")).isNull();
	}

	@Test
	public void writeAllowedWhenKeyMatches() {
		MockEnvironment environment = new MockEnvironment()
				.withProperty(VALID_KEYS_REGEX_PROPERTY, "message");
		WritableEnvironmentEndpointWebExtension extension = extension(environment);

		then(extension.write("message", "Foo"))
				.isEqualTo(Collections.singletonMap("message", "Foo"));
		then(environment.getProperty("message")).isEqualTo("Foo");
	}

	@Test
	public void validKeysRegexCannotBeWidenedAtRuntime() {
		MockEnvironment environment = new MockEnvironment()
				.withProperty(VALID_KEYS_REGEX_PROPERTY, "message");
		EnvironmentManager manager = manager(environment);
		WritableEnvironmentEndpointWebExtension extension = new WritableEnvironmentEndpointWebExtension(
				new WritableEnvironmentEndpoint(environment), manager);

		assertRejected(extension, VALID_KEYS_REGEX_PROPERTY);
		then(environment.getProperty(VALID_KEYS_REGEX_PROPERTY)).isEqualTo("message");

		manager.setProperty(VALID_KEYS_REGEX_PROPERTY, ".*");
		assertRejected(extension, "spring.cloud.bootstrap.location");
		then(environment.getProperty("spring.cloud.bootstrap.location")).isNull();
	}

	private WritableEnvironmentEndpointWebExtension extension(
			MockEnvironment environment) {
		return new WritableEnvironmentEndpointWebExtension(
				new WritableEnvironmentEndpoint(environment), manager(environment));
	}

	private EnvironmentManager manager(MockEnvironment environment) {
		EnvironmentManager manager = new EnvironmentManager(environment);
		manager.setApplicationEventPublisher(mock(ApplicationEventPublisher.class));
		return manager;
	}

	private void assertRejected(WritableEnvironmentEndpointWebExtension extension,
			String name) {
		try {
			extension.write(name, "Foo");
			fail("expected IllegalArgumentException for key " + name);
		}
		catch (IllegalArgumentException e) {
			then(e.getMessage()).isEqualTo("Invalid key " + name);
		}
	}

}
