/**
 * Optional developer-tooling facades.
 *
 * <p>These helper classes are compiled into the normal SDK for API consistency,
 * but their third-party implementations are bundled only in the {@code -all.jar}.
 * When using the normal JAR, add the corresponding upstream dependency yourself
 * before referencing a helper that exposes that library's types.</p>
 *
 * <p>{@link me.tamkungz.nilkit.tooling.DeveloperToolbox} is the exception:
 * it has no hard dependency on the optional libraries and can be used to probe
 * which capabilities are present at runtime.</p>
 */
package me.tamkungz.nilkit.tooling;
