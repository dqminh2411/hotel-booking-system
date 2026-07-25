import { useEffect, useState } from 'react';

const PLACEHOLDER_IMAGE =
  'https://images.unsplash.com/photo-1566073771259-6a8506099945?w=1200&h=800&fit=crop';

function buildGallery(images) {
  return images && images.length > 0 ? images : [{ id: 'placeholder', url: PLACEHOLDER_IMAGE }];
}

export default function HotelGallery({ images, hotelName }) {
  const [orderedImages, setOrderedImages] = useState(() => buildGallery(images));

  useEffect(() => {
    setOrderedImages(buildGallery(images));
  }, [images]);

  const mainImage = orderedImages[0];
  const thumbnails = orderedImages.slice(1, 5);
  const extraCount = orderedImages.length - 5;

  function handleImageError(event) {
    event.currentTarget.src = PLACEHOLDER_IMAGE;
  }

  function handleThumbnailClick(positionInOrderedImages) {
    setOrderedImages((current) => {
      const next = [...current];
      [next[0], next[positionInOrderedImages]] = [next[positionInOrderedImages], next[0]];
      return next;
    });
  }

  return (
    <div className="grid grid-cols-4 gap-2 md:h-80">
      <div className="col-span-4 overflow-hidden rounded-lg bg-slate-200 md:col-span-2 md:row-span-2">
        <img
          src={mainImage.url}
          alt={hotelName}
          onError={handleImageError}
          className="h-56 w-full object-cover md:h-full"
        />
      </div>

      {thumbnails.map((image, index) => {
        const isLastThumbnail = index === thumbnails.length - 1 && extraCount > 0;
        const positionInOrderedImages = index + 1;

        return (
          <button
            key={image.id}
            type="button"
            onClick={() => handleThumbnailClick(positionInOrderedImages)}
            className="relative hidden overflow-hidden rounded-lg bg-slate-200 md:block"
          >
            <img
              src={image.url}
              alt={`${hotelName} - ảnh ${index + 2}`}
              onError={handleImageError}
              className="h-full w-full object-cover"
            />
            {isLastThumbnail && (
              <span className="absolute inset-0 flex items-center justify-center bg-black/50 text-sm font-semibold text-white">
                +{extraCount}
              </span>
            )}
          </button>
        );
      })}
    </div>
  );
}