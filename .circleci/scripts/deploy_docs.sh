
#!/bin/bash

echo "Starting deployment of Dokka documentation to GitHub Pages..."
echo "Current commit: ${CIRCLE_SHA1}"
echo "Current branch: ${CIRCLE_BRANCH}"
echo "Configuring Git user details..."
git config --global user.email "viaduct-maintainers@airbnb.com"
git config --global user.name "Viaduct Bot"

echo "Cloning gh-pages branch..."
git clone --depth 1 --branch gh-pages https://x-access-token:${GITHUB_TOKEN}@github.com/rstata-projects/viaduct-real.git gh-pages-repo || \
git clone --depth 1 https://x-access-token:${GITHUB_TOKEN}@github.com/rstata-projects/viaduct-real.git gh-pages-repo

cd gh-pages-repo

if git rev-parse --verify gh-pages >/dev/null 2>&1; then
git checkout gh-pages
else
echo "Creating gh-pages branch from scratch"
git checkout --orphan gh-pages
git rm -rf .
fi

echo "Cleaning old documentation..."
rm -rf ./*

echo "Copying new Dokka docs..."
cp -R ../htmlMultiModule/* .

echo "Adding .nojekyll to disable GitHub Pages Jekyll processing"
touch .nojekyll

git add .

if git diff --cached --quiet; then
echo "✅ No changes to commit"
else
git commit -m "Update Dokka docs from commit ${CIRCLE_SHA1} [ci skip]"
git push origin gh-pages
echo "🚀 Documentation successfully deployed to GitHub Pages"
fi

echo "Deployment script completed."