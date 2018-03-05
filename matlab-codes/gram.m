function G = gram(distanceMatrix, sigma)

den = 2*sigma*sigma;
G = exp(-(distanceMatrix.^2/den));

end
