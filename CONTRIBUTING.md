# Contributing to loom

Send bug reports, fixes, and focused features for `loom`.

## Before you start

- For work beyond a trivial fix, **open an issue first**. We can agree on the
  approach before you spend time.
- Check existing issues and pull requests to avoid duplicate work.

## Development

This is a Clojure library built with `deps.edn` and the
[Clojure CLI](https://clojure.org/guides/install_clojure); Leiningen is not
required. You need a JDK and the Clojure CLI. See the README for the full set
of aliases.

```bash
clojure -M:test    # run the test suite (compiled with *warn-on-reflection* on)
```

Requirements for a mergeable change:

- **Tests first.** Add or update tests for the behavior you change. For a bug
  fix, add a regression test that fails before the fix and passes after it.
- **Green build.** The test suite passes and the build reports **zero**
  reflection warnings.
- **No scope creep.** Keep each pull request to one logical change.

## Commits and pull requests

- Follow [Conventional Commits](https://www.conventionalcommits.org/)
  (`feat:`, `fix:`, `docs:`, `refactor:`, `test:`, `chore:` …).
- Use the imperative mood. Keep the subject under about 72 characters.
- Update `CHANGELOG.md` when your change is user-visible.
- Rebase on the latest `main` before opening the pull request.

## License

By contributing, you agree to license your contributions under the project
license. See `LICENSE` or the README.
